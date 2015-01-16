# clj-diamond

对淘宝的 `dimond` 进行 clojure 封装

## Usage

- 注册你的配置
```clojure
(register-manager "group" "dataid") ;;注册一个组为 group , id为 dataid 的配置
```

- 获得你的配置
```clojure
(get-conf "group" "dataid") ;; 获取你的配置
```

- 其他
```clojure
(all-mger) ;; 获得当前所有 manager 的 map
(all-conf) ;; 获得当前所有 conf 的 map
```

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
