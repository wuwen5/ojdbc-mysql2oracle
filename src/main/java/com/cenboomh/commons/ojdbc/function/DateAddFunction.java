package com.cenboomh.commons.ojdbc.function;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * mysql: DATE_ADD(?, INTERVAL -? SECOND)
 * oracle: ? + numtodsinterval(-?,'SECOND')f
 *
 * @author wuwen
 */
public class DateAddFunction extends BaseFunction {

    private static final Logger log = Logger.getLogger(DateAddFunction.class.getName());

    private DateAddFunction() {
        super("date_add");
    }

    public static void init() {
        new DateAddFunction();
    }

    @Override
    public Boolean apply(Function function) {

        ExpressionList parameters = function.getParameters();

        List<Expression> expressions = parameters.getExpressions();

        if (expressions.get(0) instanceof JdbcParameter) {

            function.setName("? + numtodsinterval");

            IntervalExpression expression = (IntervalExpression) expressions.get(1);
            expressions.clear();

            IntervalExpression intervalExpression = new IntervalExpression(false);
            intervalExpression.setExpression(expression.getExpression());
            expressions.add(intervalExpression);
            expressions.add(new StringValue(expression.getIntervalType()));
            return true;
        } else {
            log.log(Level.WARNING, "暂不支持的函数转换. [" + function.toString() + "]");
            return false;
        }
    }
}
