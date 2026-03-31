package cn.aitplus.wcs.app.service.device;

import cn.aitplus.wcs.app.config.WmsProperties;
import cn.aitplus.wcs.common.constant.WcsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备点位配置同步服务。
 */
@Service
public class DevicePointsConfigSyncService {

    private static final Logger log = LoggerFactory.getLogger(DevicePointsConfigSyncService.class);
    private static final Charset GBK = Charset.forName("GBK");

    private final RestTemplate restTemplate;
    private final WmsProperties wmsProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    public DevicePointsConfigSyncService(RestTemplate restTemplate,
                                         WmsProperties wmsProperties,
                                         StringRedisTemplate stringRedisTemplate,
                                         ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.wmsProperties = wmsProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 从 WMS 上传目录加载设备点位配置并写入 Redis。
     *
     * @return 成功处理的点位配置文件数
     */
    public int loadDevicePointsConfigs() {
        List<String> syncDirs = resolveSyncPointsDirs();
        if (syncDirs.isEmpty()) {
            log.info("未配置 wms.sync_points_dir，跳过设备点位配置同步");
            return 0;
        }
        String uploadBaseUrl = normalizeUploadBaseUrl(wmsProperties.getUpload());
        if (!StringUtils.hasText(uploadBaseUrl)) {
            throw new IllegalStateException("未配置 wms.upload，无法同步设备点位配置");
        }

        int syncedFileCount = 0;
        Set<String> syncedDeviceIds = new HashSet<>();
        for (String syncDir : syncDirs) {
            syncedFileCount += syncDirectory(uploadBaseUrl, syncDir, syncedDeviceIds);
        }

        log.info("设备点位配置同步完成，目录数：{}，处理文件数：{}，设备数：{}",
            syncDirs.size(), syncedFileCount, syncedDeviceIds.size());
        return syncedFileCount;
    }

    private int syncDirectory(String uploadBaseUrl, String syncDir, Set<String> syncedDeviceIds) {
        String directoryUrl = resolveDirectoryUrl(uploadBaseUrl, syncDir);
        String html;
        try {
            html = restTemplate.getForObject(directoryUrl, String.class);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("设备点位配置目录 [{}] 不存在，已跳过", directoryUrl);
            return 0;
        } catch (RestClientException ex) {
            log.warn("读取设备点位配置目录失败，已跳过目录：{}", directoryUrl, ex);
            return 0;
        }

        if (!StringUtils.hasText(html)) {
            log.warn("设备点位配置目录 [{}] 返回内容为空，已跳过", directoryUrl);
            return 0;
        }

        Document document = Jsoup.parse(html, directoryUrl);
        int syncedCount = 0;
        for (Element link : document.select("a[href]")) {
            String href = link.attr("href");
            if (!isYamlHref(href)) {
                continue;
            }
            String fileUrl = link.absUrl("href");
            if (!StringUtils.hasText(fileUrl)) {
                fileUrl = resolveRelativeUrl(directoryUrl, href);
            }
            syncedCount += syncSingleFile(fileUrl, syncedDeviceIds);
        }

        if (syncedCount == 0) {
            log.info("设备点位配置目录 [{}] 未发现可同步的 YAML 文件", directoryUrl);
        }
        return syncedCount;
    }

    private int syncSingleFile(String fileUrl, Set<String> syncedDeviceIds) {
        String deviceId = extractDeviceId(fileUrl);
        if (!StringUtils.hasText(deviceId)) {
            log.warn("无法从点位配置文件 URL [{}] 提取设备编号，已跳过", fileUrl);
            return 0;
        }
        if (!syncedDeviceIds.add(deviceId)) {
            log.warn("设备 [{}] 的点位配置重复出现，后加载的文件将覆盖前面的结果，来源：{}",
                deviceId, fileUrl);
        }

        try {
            byte[] yamlBytes = downloadYamlBytes(fileUrl);
            String yamlText = sanitizeYamlText(decodeYamlText(yamlBytes));
            String jsonText = convertYamlToJson(fileUrl, yamlText);
            stringRedisTemplate.opsForValue().set(buildRedisKey(deviceId), jsonText);
            log.debug("设备 [{}] 点位配置同步完成，来源：{}", deviceId, fileUrl);
            return 1;
        } catch (IllegalStateException ex) {
            log.warn("设备 [{}] 点位配置同步失败，已跳过文件：{}", deviceId, fileUrl, ex);
            return 0;
        }
    }

    private byte[] downloadYamlBytes(String fileUrl) {
        try {
            byte[] bytes = restTemplate.getForObject(fileUrl, byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("设备点位配置文件为空：" + fileUrl);
            }
            return bytes;
        } catch (RestClientException ex) {
            throw new IllegalStateException("下载设备点位配置文件失败：" + fileUrl, ex);
        }
    }

    private String convertYamlToJson(String fileUrl, String yamlText) {
        if (!StringUtils.hasText(yamlText)) {
            throw new IllegalStateException("设备点位配置文件内容为空：" + fileUrl);
        }
        try {
            Object yamlObject = yamlObjectMapper.readValue(yamlText, Object.class);
            if (yamlObject == null) {
                throw new IllegalStateException("设备点位配置文件解析结果为空：" + fileUrl);
            }
            return objectMapper.writeValueAsString(yamlObject);
        } catch (IOException ex) {
            throw new IllegalStateException("解析设备点位配置文件失败：" + fileUrl, ex);
        }
    }

    private List<String> resolveSyncPointsDirs() {
        return normalizeDirs(wmsProperties.getSyncPointsDir());
    }

    private List<String> normalizeDirs(List<String> dirs) {
        if (dirs == null || dirs.isEmpty()) {
            return Collections.emptyList();
        }
        return dirs.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    private String normalizeUploadBaseUrl(String uploadBaseUrl) {
        if (!StringUtils.hasText(uploadBaseUrl)) {
            return "";
        }
        return ensureTrailingSlash(uploadBaseUrl.trim());
    }

    private String resolveDirectoryUrl(String uploadBaseUrl, String syncDir) {
        String trimmedDir = trimDir(syncDir);
        if (!StringUtils.hasText(trimmedDir)) {
            throw new IllegalStateException("sync_points_dir 中存在空目录配置");
        }
        if (isAbsoluteUrl(trimmedDir)) {
            return ensureTrailingSlash(trimmedDir);
        }
        return ensureTrailingSlash(URI.create(uploadBaseUrl).resolve(stripLeadingSlash(trimmedDir)).toString());
    }

    private String resolveRelativeUrl(String directoryUrl, String href) {
        return URI.create(ensureTrailingSlash(directoryUrl)).resolve(href).toString();
    }

    private String extractDeviceId(String fileUrl) {
        String path = URI.create(fileUrl).getPath();
        if (!StringUtils.hasText(path)) {
            return "";
        }
        int slashIndex = path.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".yaml")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        if (lowerCaseFileName.endsWith(".yml")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return "";
    }

    private String decodeYamlText(byte[] yamlBytes) {
        String utf8Text = new String(yamlBytes, StandardCharsets.UTF_8);
        if (utf8Text.indexOf('\uFFFD') >= 0) {
            return new String(yamlBytes, GBK);
        }
        return utf8Text;
    }

    private String sanitizeYamlText(String text) {
        String normalizedText = removeUtf8Bom(normalizeLineEndings(text));
        String[] lines = normalizedText.split("\n", -1);
        StringBuilder builder = new StringBuilder(normalizedText.length());
        for (int i = 0; i < lines.length; i++) {
            builder.append(sanitizeYamlLine(lines[i]));
            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private String sanitizeYamlLine(String line) {
        String withoutTrailingWhitespace = trimTrailingWhitespace(line);
        int contentIndex = firstNonWhitespaceIndex(withoutTrailingWhitespace);
        if (contentIndex <= 0) {
            return withoutTrailingWhitespace.replace("\t", "  ");
        }
        String indent = withoutTrailingWhitespace.substring(0, contentIndex).replace("\t", "  ");
        return indent + withoutTrailingWhitespace.substring(contentIndex);
    }

    private String trimTrailingWhitespace(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }

    private int firstNonWhitespaceIndex(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private String removeUtf8Bom(String text) {
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private boolean isYamlHref(String href) {
        if (!StringUtils.hasText(href)) {
            return false;
        }
        String lowerHref = href.toLowerCase();
        return lowerHref.endsWith(".yml") || lowerHref.endsWith(".yaml");
    }

    private boolean isAbsoluteUrl(String value) {
        String lowerValue = value.toLowerCase();
        return lowerValue.startsWith("http://") || lowerValue.startsWith("https://");
    }

    private String trimDir(String syncDir) {
        return syncDir == null ? "" : syncDir.trim();
    }

    private String stripLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private String ensureTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }

    private String buildRedisKey(String deviceId) {
        return WcsConstants.DEVICE_POINTS_CONFIG_KEY_PREFIX + deviceId;
    }
}
