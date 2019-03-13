package com.trb.android.snakeconfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnakeConfig {
    private static SnakeConfig defaultInstance;

    public static synchronized SnakeConfig getDefault() {
        synchronized (SnakeConfig.class) {
            if (defaultInstance == null) {
                defaultInstance = new SnakeConfig();
            }
        }
        return defaultInstance;
    }

    private static HashMap<String, SnakeConfig> snakeConfigHashMap;

    public static synchronized SnakeConfig getInstance(String config) {
        synchronized (SnakeConfig.class) {
            if (snakeConfigHashMap == null) {
                snakeConfigHashMap = new LinkedHashMap<>();
            }

            SnakeConfig instance = snakeConfigHashMap.get(config);
            if (instance == null) {
                instance = new SnakeConfig();
                snakeConfigHashMap.put(config, instance);
            }
            return instance;
        }
    }

    public static synchronized void removeInstance(String config) {
        synchronized (SnakeConfig.class) {
            if (snakeConfigHashMap != null) {
                snakeConfigHashMap.remove(config);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final int COMMENT = 0;
    private static final int CONFIG = 1;

    private static class ConfigItem {
        private int type;
        private String key;
        private String value;

        @Override
        public String toString() {
            return "ConfigItem{" +
                    "type=" + type +
                    ", key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private List<ConfigItem> configs;
    private boolean writable;
    private String saveFilePath;
    private Charset saveFileCharset;
    private ExecutorService threadPool;
    private final String LOCK = "SnakeConfig.LOCK";

    private SnakeConfig() {
        configs = new ArrayList<>();
        threadPool = Executors.newFixedThreadPool(1);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void printConfigItemList() {
        for (ConfigItem item : configs) {
            System.out.println(item);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 加载配置文件
     *
     * @param ins     原始配置文件的输入流
     * @param charset 原始文件的编码
     */
    public boolean load(InputStream ins, Charset charset) {
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;

        try {
            inputStreamReader = new InputStreamReader(ins, charset.name());
            reader = new BufferedReader(inputStreamReader);
            configs.clear();

            do {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                configs.add(buildConfigItem(line));
            } while (true);
            return true;
        } catch (Exception e) {
            configs.clear();
            e.printStackTrace();
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ConfigItem buildConfigItem(String line) throws Exception {
        line = line.trim();
        ConfigItem item = new ConfigItem();
        Exception exception = new Exception("Illegal line: " + line);

        if (line.isEmpty() || line.startsWith("#")) {
            item.type = COMMENT;
            item.key = "";
            item.value = line;
            return item;
        }

        int equalsSymbolFirstIndex = line.indexOf("=");
        if (equalsSymbolFirstIndex <= 0) {
            throw exception;
        }

        item.type = CONFIG;
        item.key = line.substring(0, equalsSymbolFirstIndex).trim();
        item.value = (equalsSymbolFirstIndex == line.length() - 1 ? "" : line.substring(equalsSymbolFirstIndex + 1)).trim();
        return item;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 设置存储
     *
     * @param writable     是否可写入
     * @param saveFilePath 写入文件的绝对路径
     * @param charset      写入文件的编码，一般与load时的文件编码一致
     */
    public void setStorage(boolean writable, String saveFilePath, Charset charset) {
        this.writable = writable;
        this.saveFilePath = saveFilePath;
        this.saveFileCharset = charset;
    }

    public synchronized void commitSync() {
        if (writable) {
            new WriteRunnable().run();
        }
    }

    public synchronized void commitAsync() {
        if (writable) {
            threadPool.submit(new WriteRunnable());
        }
    }

    private class WriteRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (LOCK) {
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter(saveFilePath, saveFileCharset.name());
                    StringBuilder sb = new StringBuilder();
                    for (ConfigItem item : configs) {
                        sb.append(item.key);
                        sb.append(item.type == COMMENT ? "" : "=");
                        sb.append(item.value);
                        sb.append("\n");
                    }
                    writer.write(sb.toString());
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void setProperty(String key, Object value) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    item.value = value == null ? "" : value.toString();
                    return;
                }
            }

            ConfigItem item = new ConfigItem();
            item.type = CONFIG;
            item.key = key;
            item.value = value == null ? "" : value.toString();
            configs.add(item);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void removeProperty(String key) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    configs.remove(item);
                    return;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public String getString(String key, String defaultValue) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    return item.value;
                }
            }
            return defaultValue;
        }
    }

    public byte getByte(String key, byte defaultValue) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    return Byte.valueOf(item.value);
                }
            }
            return defaultValue;
        }
    }

    public short getShort(String key, short defaultValue) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    return Short.valueOf(item.value);
                }
            }
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    return Integer.valueOf(item.value);
                }
            }
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    return Long.valueOf(item.value);
                }
            }
            return defaultValue;
        }
    }

    public double getReal(String key, double defaultValue) {
        synchronized (LOCK) {
            for (ConfigItem item : configs) {
                if (item.type == CONFIG && item.key.equals(key)) {
                    return Double.valueOf(item.value);
                }
            }
            return defaultValue;
        }
    }
}
