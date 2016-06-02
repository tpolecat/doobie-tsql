package tsql.spec

import tsql.spec.JdbcType._
import java.sql.ResultSet
import scalaz.Coyoneda

/** 
 * Witness that a column of JcbcType `J` can be read into Scala type `A`. From such a value we can
 * derive otherwise unconstrained `Read` instances.
 */
final case class BasicGet[J <: JdbcType, A](run: Coyoneda[(ResultSet, Int) => ?, A]) { outer =>

  def map[B](f: A => B): BasicGet[J, B] =
    BasicGet[J, B](run.map(f))

  /** Construct an `AdvancedGet` by adding a schema constraint. */
  def narrow[S <: String]: AdvancedGet[J, S, A] =
    AdvancedGet(run)

}

object BasicGet {

  def apply[J <: JdbcType, A](implicit ev: BasicGet[J, A]): BasicGet[J, A] =
    ev

  def instance[J <: JdbcType]: InstancePartiallyApplied[J] =
    new InstancePartiallyApplied[J]

  final class InstancePartiallyApplied[J <: JdbcType] {
    def apply[A](pget0: (ResultSet, Int) => A): BasicGet[J, A] =
      BasicGet(Coyoneda.lift[(ResultSet, Int) => ?, A](pget0))
  }

  // The JDBC Specification for reading basic types.
  implicit val TinyIntByte        = instance[JdbcTinyInt      ](_ getByte       _)
  implicit val TinyIntShort       = instance[JdbcTinyInt      ](_ getShort      _)
  implicit val SmallIntShort      = instance[JdbcSmallInt     ](_ getShort      _)
  implicit val TinyIntInt         = instance[JdbcTinyInt      ](_ getInt        _)
  implicit val SmallIntInt        = instance[JdbcSmallInt     ](_ getInt        _)
  implicit val IntegerInt         = instance[JdbcInteger      ](_ getInt        _)
  implicit val TinyIntLong        = instance[JdbcTinyInt      ](_ getLong       _)
  implicit val SmallIntLong       = instance[JdbcSmallInt     ](_ getLong       _)
  implicit val IntegerLong        = instance[JdbcInteger      ](_ getLong       _)
  implicit val BigIntLong         = instance[JdbcBigInt       ](_ getLong       _)
  implicit val TinyIntFloat       = instance[JdbcTinyInt      ](_ getFloat      _)
  implicit val SmallIntFloat      = instance[JdbcSmallInt     ](_ getFloat      _)
  implicit val IntegerFloat       = instance[JdbcInteger      ](_ getFloat      _)
  implicit val BigIntFloat        = instance[JdbcBigInt       ](_ getFloat      _)
  implicit val RealFloat          = instance[JdbcReal         ](_ getFloat      _)
  implicit val DecimalFloat       = instance[JdbcDecimal      ](_ getFloat      _)
  implicit val NumericFloat       = instance[JdbcNumeric      ](_ getFloat      _)
  implicit val TinyIntDouble      = instance[JdbcTinyInt      ](_ getDouble     _)
  implicit val SmallIntDouble     = instance[JdbcSmallInt     ](_ getDouble     _)
  implicit val IntegerDouble      = instance[JdbcInteger      ](_ getDouble     _)
  implicit val BigIntDouble       = instance[JdbcBigInt       ](_ getDouble     _)
  implicit val RealDouble         = instance[JdbcReal         ](_ getDouble     _)
  implicit val DoubleDouble       = instance[JdbcDouble       ](_ getDouble     _)
  implicit val DecimalDouble      = instance[JdbcDecimal      ](_ getDouble     _)
  implicit val NumericDouble      = instance[JdbcNumeric      ](_ getDouble     _)
  implicit val DecimalBigDecimal  = instance[JdbcDecimal      ](_ getBigDecimal _)
  implicit val NumericBigDecimal  = instance[JdbcNumeric      ](_ getBigDecimal _)
  implicit val BitBoolean         = instance[JdbcBit          ](_ getBoolean    _)
  implicit val BooleanBoolean     = instance[JdbcBoolean      ](_ getBoolean    _)
  implicit val CharString         = instance[JdbcChar         ](_ getString     _)
  implicit val VarCharString      = instance[JdbcVarChar      ](_ getString     _)
  implicit val LongVarCharString  = instance[JdbcLongVarChar  ](_ getString     _)
  implicit val BinaryByte         = instance[JdbcBinary       ](_ getBytes      _)
  implicit val VarBinaryByte      = instance[JdbcVarBinary    ](_ getBytes      _)
  implicit val LongVarBinaryByte  = instance[JdbcLongVarBinary](_ getBytes      _)
  implicit val DateDate           = instance[JdbcDate         ](_ getDate       _)
  implicit val TimeTime           = instance[JdbcTime         ](_ getTime       _)
  implicit val TimestampTimestamp = instance[JdbcTimestamp    ](_ getTimestamp  _)

  // Advanced types that will normally require additional constraints and narrowing
  val Array = instance[JdbcArray](_ getArray _)

}

