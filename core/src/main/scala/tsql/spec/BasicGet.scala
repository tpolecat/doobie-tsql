package tsql.spec

import tsql.spec.JdbcType._
import java.sql.ResultSet

trait BasicGet[J <: JdbcType, A] { outer =>

  def unsafeRead(rs: ResultSet, n: Int): A

  /** BasicGet.Array.narrow[Witness.`"_int4"`.T] */
  def narrow[S <: String with Singleton]: AdvancedGet[J, S, A] =
    new AdvancedGet[J, S, A] {
      type I = A
      val basic = outer
      val ia: I => A = identity
    }

  // TODO: map

}

object BasicGet {

  def apply[J <: JdbcType, A](implicit ev: BasicGet[J, A]): BasicGet[J, A] =
    ev

  def instance[J <: JdbcType]: InstancePartiallyApplied[J] =
    new InstancePartiallyApplied[J]

  final class InstancePartiallyApplied[J <: JdbcType] {
    def apply[A](unsafeGet: (ResultSet, Int) => A): BasicGet[J, A] =
      new BasicGet[J, A] {
        def unsafeRead(rs: ResultSet, n: Int): A = unsafeGet(rs, n)
      }
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

