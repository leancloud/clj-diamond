# clj-diamond

对淘宝的 `dimond` 进行 clojure 封装

## Usage

- 注册你的配置
```clojure
;; 普通配置添加
(add-manager ["group" "data-id"]) ;;注册一个组为 group , id为 dataid 的配置，且默认为 string 类型
(add-manager ["group" "data-id" :clojure (fn callback [data] (do some thing)) :sync-timeout 1000 :sync-cb true]) ;;可以添加的配置项
;; 将你配置的字符串的格式转化为clojure的数据结构（可选的有 `:clojure` `:json` `property` `yml`）
;; 单一配置添加
(single-manager ["group" "data-id"]) ;;参数和函数 `add-manager` 相同，假如你只有一个配置时，你可以直接使用 `env` 函数来获取你的配置
(single-manager ["gourp" "data-id" :json]) ;;获取你的配置，并将你的 `json` 字符串转化为 `clojure map` （map 的 key 为 keyword）
```

- 获得你的配置
```clojure
;; 普通配置添加
(get-conf "group" "dataid") ;; 获取你的配置
;; 单一配置添加
(env :my-key default) ;;获取你的配置，无任何转换
```

- 其他
```clojure
;; 比如有配置 group:"testgroup" dataid:"testdataid" 内容为 {:a 1}
;; `with-current` 宏会 `binding` group 和 dataid
(with-current "testgroup" "testdataid" (inc (env :a 0))) ;;结果为 2
(all-mger) ;; 获得当前所有 manager 的 map
(all-conf) ;; 获得当前所有 conf 的 map
(all-type) ;; 获取当前所有 conf 的 map
```

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
