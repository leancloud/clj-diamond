# clj-diamond

对淘宝的 `dimond-client` 进行 clojure 封装

## Usage

- 添加依赖
```clojure
    [clj-diamond "0.1.3-SNAPSHOT"]
```

- 注册你的配置
```clojure
;; 普通配置添加
(add-manager ["group" "data-id"]) ;;注册一个组为 group , id为 dataid 的配置，且默认为 string 类型
(add-manager ["group" "data-id"]
             ["group1" "data-id1"]) ;;可注册多个 manager ，但是返回nil
(add-manager ["group" "data-id" :clojure (fn callback [data] (do some thing)) :sync-timeout 1000 :sync-cb true]) ;;可以添加的配置项
;; 将你配置的字符串的格式转化为clojure的数据结构（可选的有 `:clojure` `:json` `property` `yml`）
;; 单一配置添加
(def mymanager (add-manager* ["group" "data-id"])) ;;返回一个配置元信息
(set-single-manager! mymanager) ;;设定一个全局配置，你可以直接使用 `env` 函数来获取你的配置
(def myjsonconf (add-manager* ["gourp" "data-id" :json])) ;;获取你的配置，并将你的 `json` 字符串转化为 `clojure map` （map 的 key 为 keyword）
```

- 获得你的配置
```clojure
;; 普通配置
(get-conf "group" "dataid") ;; 获取你的配置
;; 单一配置
(env :my-key default) ;;获取你的配置，无任何转换
```

- 其他
```clojure
;; 比如有配置 group:"testgroup" dataid:"testdataid" 内容为 {:a 1}
;; `with-current` 宏会 `binding` group 和 dataid
(def mymanager (add-manager* ["group" "data-id" :clojure]))
(with-current mymanager (inc (env :a 0))) ;;结果为 2
(with-current mymanager (inc (env :b 0))) ;;结果为 1
(all-mger) ;; 获得当前所有 manager 的 map
(all-conf) ;; 获得当前所有 conf 的 map
(all-type) ;; 获取当前所有 conf 的 map
```

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
