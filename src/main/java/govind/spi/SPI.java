package govind.spi;

import java.lang.annotation.*;

/**
 * 标注对应接口为可扩展接口
 *
 * 例如：接口com.xxx.Protocol的具体实现可以在 META-INF/spi/com.xxx.Protocol文件
 * 中配置，配置内容为K-V键值对：
 * <pre>
 *     xxx=com.impl.XxxProtocol
 *     yyy=com.impl.YyyProtocol
 * </pre>
 * 而不是采用Java SPI默认的在META-INF/services/com.xxx.Protocol中的配置格式：
 * <pre>
 *     com.impl.XxxProtocol
 *     com.impl.YyyProtocol
 * </pre>
 *
 * 因为若采用Java SPI默认机制，当引用的第三方jar中没有对应实现类时，Java会抛出ClassNotFound异常
 * 而不知道具体是哪一个实现类没有找到，通过采用KV键值对方式当没有找到@SPI("mima")对应的类时会直接
 * 抛出：未能加载Extection("mima")的错误信息，这样更容易定位问题。
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {
	/**
	 * 默认扩展类的Key
	 *
	 * 若没有指定值，默认名称为类名按照驼峰规则将每个单词分隔，然后变为小写后采用dot进行连接。
	 *
	 * 例如：{@code org.apache.dubbo.xxx.YyyInvokerWrapper}类若使用{@link SPI}进行注解，
	 * 则默认value值为：yyy.invoker.wrapper，在后续进行依赖注入时会根据该值查看扩展类名，具体
	 * 参考 {@link Adaptive}
	 */
	String value() default "";
}
