# MySQL Data Types

MySQL supports a number of SQL data types in several categories: numeric types, date and time types, string (character and byte) types, spatial types, and the JSON data type. This chapter provides an overview of these data types, a more detailed description of the properties of the types in each category, and a summary of the data type storage requirements. The initial overview is intentionally brief. The more detailed descriptions later in the chapter should be consulted for additional information about particular data types, such as the permissible formats in which you can specify values.

Data type descriptions use these conventions:

• M indicates the maximum display width for integer types. For floating-point and fixed-point types, M is the total number of digits that can be stored (the precision). For string types, M is the maximum length. The maximum permissible value of M depends on the data type.

• D applies to floating-point and fixed-point types and indicates the number of digits following the decimal point (the scale). The maximum possible value is 30, but should be no greater than M−2.

• fsp applies to the TIME, DATETIME, and TIMESTAMP types and represents fractional seconds precision; that is, the number of digits following the decimal point for fractional parts of seconds. The fsp value, if given, must be in the range 0 to 6. A value of 0 signifies that there is no fractional part. If omitted, the default precision is 0. (This differs from the standard SQL default of 6, for compatibility with previous MySQL versions.)

• Square brackets ( [ and ] ) indicate optional parts of type definitions.

## Data Type Overview

### Numeric Type Overview

M indicates the maximum display width for integer types. The maximum display width is 255. Display width is unrelated to the range of values a type can contain. For floating-point and fixed-point types, M is the total number of digits that can be stored.

If you specify ZEROFILL for a numeric column, MySQL automatically adds the UNSIGNED attribute to the column.

Numeric data types that permit the UNSIGNED attribute also permit SIGNED. However, these data types are signed by default, so the SIGNED attribute has no effect.

**SERIAL** is an alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE. SERIAL DEFAULT VALUE in the definition of an integer column is an alias for NOT NULL AUTO_INCREMENT UNIQUE.

> **Warning**
> When you use subtraction between integer values where one is of type UNSIGNED, the result is unsigned unless the NO_UNSIGNED_SUBTRACTION SQL mode is enabled. 

**• BIT[(M)]**

A bit-value type. M indicates the number of bits per value, from 1 to 64. The default is 1 if M is omitted.

**• TINYINT\[(M)]\[UNSIGNED][ZEROFILL]**

A very small integer. The signed range is -128 to 127. The unsigned range is 0 to 255.

**• BOOL, BOOLEAN**
These types are synonyms for TINYINT(1). A value of zero is considered false. Nonzero values are considered true:

```sql
mysql> SELECT IF(0, 'true', 'false');
+------------------------+
| IF(0, 'true', 'false') |
+------------------------+
| false                  |
+------------------------+

mysql> SELECT IF(1, 'true', 'false');
+------------------------+
| IF(1, 'true', 'false') |
+------------------------+
| true                   |
+------------------------+

mysql> SELECT IF(2, 'true', 'false');
+------------------------+
| IF(2, 'true', 'false') |
+------------------------+
| true                   |
+------------------------+
```

However, the values TRUE and FALSE are merely aliases for 1 and 0, respectively, as shown here:

```sql
mysql> SELECT IF(0 = FALSE, 'true', 'false');
+--------------------------------+
| IF(0 = FALSE, 'true', 'false') |
+--------------------------------+
| true                           |
+--------------------------------+

mysql> SELECT IF(1 = TRUE, 'true', 'false');
+-------------------------------+
| IF(1 = TRUE, 'true', 'false') |
+-------------------------------+
| true                          |
+-------------------------------+

mysql> SELECT IF(2 = TRUE, 'true', 'false');
+-------------------------------+
| IF(2 = TRUE, 'true', 'false') |
+-------------------------------+
| false                         |
+-------------------------------+

mysql> SELECT IF(2 = FALSE, 'true', 'false');
+--------------------------------+
| IF(2 = FALSE, 'true', 'false') |
+--------------------------------+
| false                          |
+--------------------------------+
1 row in set (0.00 sec)
```

The last two statements display the results shown because 2 is equal to neither 1 nor 0.

**• SMALLINT\[(M)]\[UNSIGNED][ZEROFILL]**

A small integer. The signed range is -32768 to 32767. The unsigned range is 0 to 65535.

**• MEDIUMINT\[(M)]\[UNSIGNED][ZEROFILL]**

A medium-sized integer. The signed range is -8388608 to 8388607. The unsigned range is 0 to 16777215.

**• INT\[(M)]\[UNSIGNED][ZEROFILL]**

A normal-size integer. The signed range is -2147483648 to 2147483647. The unsigned range is 0 to 4294967295.

**• INTEGER\[(M)]\[UNSIGNED][ZEROFILL]**

This type is a synonym for INT.

**• BIGINT\[(M)]\[UNSIGNED][ZEROFILL]**

A large integer. The signed range is -9223372036854775808 to 9223372036854775807. The unsigned range is 0 to 18446744073709551615.

Some things you should be aware of with respect to BIGINT columns:

- All arithmetic is done using signed BIGINT or DOUBLE values, so you should not use unsigned big integers larger than 9223372036854775807 (63 bits) except with bit functions! If you do that, some of the last digits in the result may be wrong because of rounding errors when converting a BIGINT value to a DOUBLE. MySQL can handle BIGINT in the following cases:

  + When using integers to store large unsigned values in a BIGINT column.

  + In MIN(col_name) or MAX(col_name), where col_name refers to a BIGINT column.

  + When using operators (+, -, *, and so on) where both operands are integers.

- You can always store an exact integer value in a BIGINT column by storing it using a string. In this case, MySQL performs a string-to-number conversion that involves no intermediate doubleprecision representation.

- The -, +, and * operators use BIGINT arithmetic when both operands are integer values. This means that if you multiply two big integers (or results from functions that return integers), you may get unexpected results when the result is larger than 9223372036854775807.

**• DECIMAL\[(M\[,D])]\[UNSIGNED][ZEROFILL]**

A packed “exact” fixed-point number. M is the total number of digits (the precision) and D is the number of digits after the decimal point (the scale). The decimal point and (for negative numbers) the - sign are not counted in M. If D is 0, values have no decimal point or fractional part. The maximum number of digits (M) for DECIMAL is 65. The maximum number of supported decimals (D) is 30. If D is omitted, the default is 0. If M is omitted, the default is 10.

UNSIGNED, if specified, disallows negative values.

All basic calculations (+, -, *, /) with DECIMAL columns are done with a precision of 65 digits.

**• DEC\[(M\[,D])]\[UNSIGNED][ZEROFILL], NUMERIC\[(M\[,D])]\[UNSIGNED][ZEROFILL], FIXED\[(M\[,D])]\[UNSIGNED][ZEROFILL]**

These types are synonyms for DECIMAL. The FIXED synonym is available for compatibility with other database systems.

**• FLOAT\[(M,D)]\[UNSIGNED][ZEROFILL]**

A small (single-precision) floating-point number. Permissible values are -3.402823466E+38 to -1.175494351E-38, 0, and 1.175494351E-38 to 3.402823466E+38. These are the theoretical limits, based on the IEEE standard. The actual range might be slightly smaller depending on your hardware or operating system.

M is the total number of digits and D is the number of digits following the decimal point. If M and D are omitted, values are stored to the limits permitted by the hardware. A single-precision floating-point number is accurate to approximately 7 decimal places.

UNSIGNED, if specified, disallows negative values.

Using FLOAT might give you some unexpected problems because all calculations in MySQL are done with double precision.

**• DOUBLE\[(M,D)]\[UNSIGNED][ZEROFILL]**

A normal-size (double-precision) floating-point number. Permissible values are -1.7976931348623157E+308 to -2.2250738585072014E-308, 0, and 2.2250738585072014E-308 to 1.7976931348623157E+308. These are the theoretical limits, based on the IEEE standard. The actual range might be slightly smaller depending on your hardware or operating system.

M is the total number of digits and D is the number of digits following the decimal point. If M and D are omitted, values are stored to the limits permitted by the hardware. A double-precision floating-point
number is accurate to approximately 15 decimal places.

UNSIGNED, if specified, disallows negative values.

**• DOUBLE PRECISION\[(M,D)]\[UNSIGNED][ZEROFILL], REAL\[(M,D)]\[UNSIGNED][ZEROFILL]**

These types are synonyms for DOUBLE. Exception: If the REAL_AS_FLOAT SQL mode is enabled, REAL is a synonym for FLOAT rather than DOUBLE.

**• FLOAT(p) \[UNSIGNED][ZEROFILL]**

A floating-point number. p represents the precision in bits, but MySQL uses this value only to determine whether to use FLOAT or DOUBLE for the resulting data type. If p is from 0 to 24, the data type becomes FLOAT with no M or D values. If p is from 25 to 53, the data type becomes DOUBLE with no M or D values. The range of the resulting column is the same as for the single-precision FLOAT or double-precision DOUBLE data types described earlier in this section.

FLOAT(p) syntax is provided for ODBC compatibility.

### Date and Time Type Overview

MySQL permits fractional seconds for TIME, DATETIME, and TIMESTAMP values, with up to microseconds (6 digits) precision. To define a column that includes a fractional seconds part, use the syntax type_name(fsp), where type_name is TIME, DATETIME, or TIMESTAMP, and fsp is the fractional seconds precision. For example:

```sql
CREATE TABLE t1 (t TIME(3), dt DATETIME(6));
```

The fsp value, if given, must be in the range 0 to 6. A value of 0 signifies that there is no fractional part. If omitted, the default precision is 0. (This differs from the standard SQL default of 6, for compatibility with previous MySQL versions.)

Any TIMESTAMP or DATETIME column in a table can have automatic initialization and updating properties.

**• DATE**

A date. The supported range is '1000-01-01' to '9999-12-31'. MySQL displays DATE values in 'YYYY-MM-DD' format, but permits assignment of values to DATE columns using either strings or numbers.

**• DATETIME[(fsp)]**

A date and time combination. The supported range is '1000-01-01 00:00:00.000000' to '9999-12-31 23:59:59.999999'. MySQL displays DATETIME values in 'YYYY-MM-DD HH:MM:SS[.fraction]' format, but permits assignment of values to DATETIME columns using either strings or numbers.

An optional fsp value in the range from 0 to 6 may be given to specify fractional seconds precision. A value of 0 signifies that there is no fractional part. If omitted, the default precision is 0. Automatic initialization and updating to the current date and time for DATETIME columns can be specified using DEFAULT and ON UPDATE column definition clauses.

**• TIMESTAMP[(fsp)]**

A timestamp. The range is '1970-01-01 00:00:01.000000' UTC to '2038-01-19 03:14:07.999999' UTC. TIMESTAMP values are stored as the number of seconds since the epoch ('1970-01-01 00:00:00' UTC). A TIMESTAMP cannot represent the value '1970-01-01 00:00:00' because that is equivalent to 0 seconds from the epoch and the value 0 is reserved for representing '0000-00-00 00:00:00', the “zero” TIMESTAMP value.

An optional fsp value in the range from 0 to 6 may be given to specify fractional seconds precision. A value of 0 signifies that there is no fractional part. If omitted, the default precision is 0. The way the server handles TIMESTAMP definitions depends on the value of the explicit_defaults_for_timestamp system variable.

If explicit_defaults_for_timestamp is enabled, there is no automatic assignment of the DEFAULT CURRENT_TIMESTAMP or ON UPDATE CURRENT_TIMESTAMP attributes to any TIMESTAMP column. They must be included explicitly in the column definition. Also, any TIMESTAMP not explicitly declared as NOT NULL permits NULL values.

If explicit_defaults_for_timestamp is disabled, the server handles TIMESTAMP as follows:

Unless specified otherwise, the first TIMESTAMP column in a table is defined to be automatically set to the date and time of the most recent modification if not explicitly assigned a value. This makes TIMESTAMP useful for recording the timestamp of an INSERT or UPDATE operation. You can also set any TIMESTAMP column to the current date and time by assigning it a NULL value, unless it has been defined with the NULL attribute to permit NULL values.

Automatic initialization and updating to the current date and time can be specified using DEFAULT
CURRENT_TIMESTAMP and ON UPDATE CURRENT_TIMESTAMP column definition clauses. By default, the first TIMESTAMP column has these properties, as previously noted. However, any TIMESTAMP column in a table can be defined to have these properties.

**• TIME[(fsp)]**

A time. The range is '-838:59:59.000000' to '838:59:59.000000'. MySQL displays TIME values in 'HH:MM:SS[.fraction]' format, but permits assignment of values to TIME columns using either strings or numbers.

An optional fsp value in the range from 0 to 6 may be given to specify fractional seconds precision. A value of 0 signifies that there is no fractional part. If omitted, the default precision is 0.

**• YEAR[(4)]**

A year in four-digit format. MySQL displays YEAR values in YYYY format, but permits assignment of values to YEAR columns using either strings or numbers. Values display as 1901 to 2155, and 0000.

> Note
> The YEAR(2) data type is deprecated and support for it is removed in MySQL 5.7.5.

The SUM() and AVG() aggregate functions do not work with temporal values. (They convert the values to numbers, losing everything after the first nonnumeric character.) To work around this problem, convert to numeric units, perform the aggregate operation, and convert back to a temporal value. Examples:

```sql
SELECT SEC_TO_TIME(SUM(TIME_TO_SEC(time_col))) FROM tbl_name;
SELECT FROM_DAYS(SUM(TO_DAYS(date_col))) FROM tbl_name;
```

> **Note**
> The MySQL server can be run with the MAXDB SQL mode enabled. In this case, TIMESTAMP is identical with DATETIME. If this mode is enabled at the time that a table is created, TIMESTAMP columns are created as DATETIME columns. As a result, such columns use DATETIME display format, have the same range of values, and there is no automatic initialization or updating to the current date and time. 

### String Type Overview

In some cases, MySQL may change a string column to a type different from that given in a CREATE TABLE or ALTER TABLE statement.

MySQL interprets length specifications in character column definitions in character units. This applies to CHAR, VARCHAR, and the TEXT types.

Column definitions for many string data types can include attributes that specify the character set or collation of the column. These attributes apply to the CHAR, VARCHAR, the TEXT types, ENUM, and SET
data types:

• The **CHARACTER SET** attribute specifies the character set, and the COLLATE attribute specifies a collation for the character set. For example:

```sql
CREATE TABLE t (
	c1 VARCHAR(20) CHARACTER SET utf8,
	c2 TEXT CHARACTER SET latin1 COLLATE latin1_general_cs
);
```

This table definition creates a column named c1 that has a character set of utf8 with the default collation for that character set, and a column named c2 that has a character set of latin1 and a case-sensitive collation.

CHARSET is a synonym for CHARACTER SET.

• Specifying the **CHARACTER SET binary** attribute for a character string data type causes the column to be created as the corresponding binary string data type: CHAR becomes BINARY, VARCHAR becomes VARBINARY, and TEXT becomes BLOB. For the ENUM and SET data types, this does not occur; they are created as declared. Suppose that you specify a table using this definition:

```sql
CREATE TABLE t (
	c1 VARCHAR(10) CHARACTER SET binary,
	c2 TEXT CHARACTER SET binary,
	c3 ENUM('a','b','c') CHARACTER SET binary
);
```

The resulting table has this definition:

```sql
CREATE TABLE t (
	c1 VARBINARY(10),
	c2 BLOB,
	c3 ENUM('a','b','c') CHARACTER SET binary
);
```

• The **BINARY** attribute is shorthand for specifying the table default character set and the binary (\_bin) collation of that character set. In this case, comparison and sorting are based on numeric character code values.

• The **ASCII** attribute is shorthand for CHARACTER SET latin1.

• The **UNICODE** attribute is shorthand for CHARACTER SET ucs2.

Character column comparison and sorting are based on the collation assigned to the column. For the CHAR, VARCHAR, TEXT, ENUM, and SET data types, you can declare a column with a binary (_bin) collation or the BINARY attribute to cause comparison and sorting to use the underlying character code values rather than a lexical ordering.

**• [NATIONAL] CHAR\[(M)]\[CHARACTER SET charset_name][COLLATE collation_name]**

A fixed-length string that is always right-padded with spaces to the specified length when stored. M represents the column length in characters. The range of M is 0 to 255. If M is omitted, the length is 1.

> **Note**
> Trailing spaces are removed when CHAR values are retrieved unless the PAD_CHAR_TO_FULL_LENGTH SQL mode is enabled.

CHAR is shorthand for CHARACTER. NATIONAL CHAR (or its equivalent short form, NCHAR) is the standard SQL way to define that a CHAR column should use some predefined character set. MySQL uses utf8 as this predefined character set.

The CHAR BYTE data type is an alias for the BINARY data type. This is a compatibility feature. MySQL permits you to create a column of type CHAR(0). This is useful primarily when you have to be compliant with old applications that depend on the existence of a column but that do not actually use its value. CHAR(0) is also quite nice when you need a column that can take only two values: A column that is defined as CHAR(0) NULL occupies only one bit and can take only the values NULL and '' (the empty string).

**• [NATIONAL] VARCHAR(M) \[CHARACTER SET charset_name][COLLATE collation_name]**

A variable-length string. M represents the maximum column length in characters. The range of M is 0 to 65,535. The effective maximum length of a VARCHAR is subject to the maximum row size (65,535 bytes, which is shared among all columns) and the character set used. For example, utf8 characters can require up to three bytes per character, so a VARCHAR column that uses the utf8 character set can be declared to be a maximum of 21,844 characters.

MySQL stores VARCHAR values as a 1-byte or 2-byte length prefix plus data. The length prefix indicates the number of bytes in the value. A VARCHAR column uses one length byte if values require no more than 255 bytes, two length bytes if values may require more than 255 bytes.

> Note
> MySQL follows the standard SQL specification, and does not remove trailing spaces from VARCHAR values.

VARCHAR is shorthand for CHARACTER VARYING. NATIONAL VARCHAR is the standard SQL way to define that a VARCHAR column should use some predefined character set. MySQL uses utf8 as this predefined character set.

**• BINARY[(M)]**

The BINARY type is similar to the CHAR type, but stores binary byte strings rather than nonbinary character strings. An optional length M represents the column length in bytes. If omitted, M defaults to 1.

**• VARBINARY(M)**

The VARBINARY type is similar to the VARCHAR type, but stores binary byte strings rather than nonbinary character strings. M represents the maximum column length in bytes.

**• TINYBLOB**

A BLOB column with a maximum length of 255 (2<sup>8</sup> − 1) bytes. Each TINYBLOB value is stored using a 1-byte length prefix that indicates the number of bytes in the value.

**• TINYTEXT \[CHARACTER SET charset_name][COLLATE collation_name]**

A TEXT column with a maximum length of 255 (2<sup>8</sup> − 1) characters. The effective maximum length is less if the value contains multibyte characters. Each TINYTEXT value is stored using a 1-byte length prefix that indicates the number of bytes in the value.

**• BLOB[(M)]**

A BLOB column with a maximum length of 65,535 (2<sup>16</sup> − 1) bytes. Each BLOB value is stored using a 2-byte length prefix that indicates the number of bytes in the value.

An optional length M can be given for this type. If this is done, MySQL creates the column as the smallest BLOB type large enough to hold values M bytes long.

**• TEXT\[(M)]\[CHARACTER SET charset_name][COLLATE collation_name]**

A TEXT column with a maximum length of 65,535 (2<sup>16</sup> − 1) characters. The effective maximum length is less if the value contains multibyte characters. Each TEXT value is stored using a 2-byte length prefix that indicates the number of bytes in the value.

An optional length M can be given for this type. If this is done, MySQL creates the column as the smallest TEXT type large enough to hold values M characters long.

**• MEDIUMBLOB**

A BLOB column with a maximum length of 16,777,215 (2<sup>24</sup> − 1) bytes. Each MEDIUMBLOB value is stored using a 3-byte length prefix that indicates the number of bytes in the value.

**• MEDIUMTEXT \[CHARACTER SET charset_name][COLLATE collation_name]**

A TEXT column with a maximum length of 16,777,215 (2<sup>24 </sup>− 1) characters. The effective maximum length is less if the value contains multibyte characters. Each MEDIUMTEXT value is stored using a 3-byte length prefix that indicates the number of bytes in the value.

**• LONGBLOB**
A BLOB column with a maximum length of 4,294,967,295 or 4GB (2<sup>32</sup> − 1) bytes. The effective maximum length of LONGBLOB columns depends on the configured maximum packet size in the client/server protocol and available memory. Each LONGBLOB value is stored using a 4-byte length prefix that indicates the number of bytes in the value.

**• LONGTEXT \[CHARACTER SET charset_name][COLLATE collation_name]**

A TEXT column with a maximum length of 4,294,967,295 or 4GB (2<sup>32</sup> − 1) characters. The effective maximum length is less if the value contains multibyte characters. The effective maximum length of LONGTEXT columns also depends on the configured maximum packet size in the client/server protocol and available memory. Each LONGTEXT value is stored using a 4-byte length prefix that indicates the number of bytes in the value.

**• ENUM('value1','value2',...) \[CHARACTER SET charset_name][COLLATE collation_name]**

An enumeration. A string object that can have only one value, chosen from the list of values 'value1', 'value2', ..., NULL or the special '' error value. ENUM values are represented internally as integers.

An ENUM column can have a maximum of 65,535 distinct elements. (The practical limit is less than 3000.) A table can have no more than 255 unique element list definitions among its ENUM and SET columns considered as a group.

**• SET('value1','value2',...) \[CHARACTER SET charset_name][COLLATE collation_name]**

A set. A string object that can have zero or more values, each of which must be chosen from the list of values 'value1', 'value2', ... SET values are represented internally as integers.

A SET column can have a maximum of 64 distinct members. A table can have no more than 255 unique element list definitions among its ENUM and SET columns considered as a group.

## Numeric Types

MySQL supports all standard SQL numeric data types. These types include the exact numeric data types (INTEGER, SMALLINT, DECIMAL, and NUMERIC), as well as the approximate numeric data types (FLOAT, REAL, and DOUBLE PRECISION). The keyword INT is a synonym for INTEGER, and the keywords DEC and FIXED are synonyms for DECIMAL. MySQL treats DOUBLE as a synonym for DOUBLE PRECISION (a nonstandard extension). MySQL also treats REAL as a synonym for DOUBLE PRECISION (a nonstandard variation), unless the REAL_AS_FLOAT SQL mode is enabled.

The BIT data type stores bit values and is supported for MyISAM, MEMORY, InnoDB, and NDB tables. 

The data type used for the result of a calculation on numeric operands depends on the types of the
operands and the operations performed on them. 

### Integer Types (Exact Value) - INTEGER, INT, SMALLINT, TINYINT, MEDIUMINT,BIGINT

MySQL supports the SQL standard integer types INTEGER (or INT) and SMALLINT. As an extension to the standard, MySQL also supports the integer types TINYINT, MEDIUMINT, and BIGINT. The following table shows the required storage and range for each integer type.

| Type      | Storage(Bytes) | Minimum Value(Signed/Unsigned) | Maximum Value(Signed/Unsigned)               |
| --------- | -------------- | ------------------------------ | -------------------------------------------- |
| TINYINT   | 1              | -128/0                         | 127/255                                      |
| SMALLINT  | 2              | -32768/0                       | 32767/65535                                  |
| MEDIUMINT | 3              | -8388608/0                     | 8388607/16777215                             |
| INT       | 4              | -2147483648/0                  | 2147483647/4294967295                        |
| BIGINT    | 8              | -9223372036854775808/0         | 9223372036854775807/<br>18446744073709551615 |

### Fixed-Point Types (Exact Value) - DECIMAL, NUMERIC 

The DECIMAL and NUMERIC types store exact numeric data values. These types are used when it is important to preserve exact precision, for example with monetary data. In MySQL, NUMERIC is implemented as DECIMAL, so the following remarks about DECIMAL apply equally to NUMERIC.
MySQL stores DECIMAL values in binary format.

In a DECIMAL column declaration, the precision and scale can be (and usually is) specified; for example:

```sql
salary DECIMAL(5,2)
```

In this example, 5 is the precision and 2 is the scale. The precision represents the number of significant digits that are stored for values, and the scale represents the number of digits that can be stored following the decimal point.

Standard SQL requires that DECIMAL(5,2) be able to store any value with five digits and two decimals, so values that can be stored in the salary column range from **-999.99 to 999.99**.

In standard SQL, the syntax DECIMAL(M) is equivalent to DECIMAL(M,0). Similarly, the syntax DECIMAL is equivalent to DECIMAL(M,0), where the implementation is permitted to decide the value of M. MySQL supports both of these variant forms of DECIMAL syntax. The default value of M is 10.

If the scale is 0, DECIMAL values contain no decimal point or fractional part.

The maximum number of digits for DECIMAL is 65, but the actual range for a given DECIMAL column can be constrained by the precision or scale for a given column. When such a column is assigned a value with more digits following the decimal point than are permitted by the specified scale, the value is converted to that scale. (The precise behavior is operating system-specific, but generally the effect is truncation to the permissible number of digits.)

### Floating-Point Types (Approximate Value) - FLOAT, DOUBLE 

The FLOAT and DOUBLE types represent approximate numeric data values. MySQL uses four bytes for single-precision values and eight bytes for double-precision values.

For FLOAT, the SQL standard permits an optional specification of the precision (but not the range of the exponent) in bits following the keyword FLOAT in parentheses. MySQL also supports this optional precision specification, but the precision value is used only to determine storage size. A precision from 0 to 23 results in a 4-byte single-precision FLOAT column. A precision from 24 to 53 results in an 8-byte double-precision DOUBLE column.

MySQL permits a nonstandard syntax: FLOAT(M,D) or REAL(M,D) or DOUBLE PRECISION(M,D). Here, (M,D) means than values can be stored with up to M digits in total, of which D digits may be after the decimal point. For example, a column defined as FLOAT(7,4) will look like -999.9999 when displayed. MySQL performs **rounding** when storing values, so if you insert 999.00009 into a FLOAT(7,4) column, the approximate result is 999.0001.

Because floating-point values are approximate and not stored as exact values, attempts to treat them as exact in comparisons may lead to problems. They are also subject to platform or implementation dependencies.

For maximum portability, code requiring storage of approximate numeric data values should use FLOAT
or DOUBLE PRECISION with no specification of precision or number of digits.

### Bit-Value Type - BIT

The BIT data type is used to store bit values. A type of BIT(M) enables storage of M-bit values. M can range from 1 to 64.

To specify bit values, b'value' notation can be used. value is a binary value written using zeros and ones. For example, b'111' and b'10000000' represent 7 and 128, respectively.

If you assign a value to a BIT(M) column that is less than M bits long, the value is padded on the left with zeros. For example, assigning a value of b'101' to a BIT(6) column is, in effect, the same as assigning b'000101'.

### Numeric Type Attributes

MySQL supports an extension for optionally specifying the display width of integer data types in parentheses following the base keyword for the type. For example, INT(4) specifies an INT with a display width of four digits. This optional display width may be used by applications to display integer values having a width less than the width specified for the column by left-padding them with spaces. (That is, this width is present in the metadata returned with result sets. Whether it is used or not is up to the application.)

The display width does not constrain the range of values that can be stored in the column. Nor does it prevent values wider than the column display width from being displayed correctly. For example, a
column specified as SMALLINT(3) has the usual SMALLINT range of -32768 to 32767, and values outside the range permitted by three digits are displayed in full using more than three digits.

When used in conjunction with the optional (nonstandard) attribute ZEROFILL, the default padding of spaces is replaced with zeros. For example, for a column declared as INT(4) ZEROFILL, a value of 5 is retrieved as 0005.

> **Note**
>
> The ZEROFILL attribute is ignored when a column is involved in expressions or UNION queries.
>
> If you store values larger than the display width in an integer column that has the ZEROFILL attribute, you may experience problems when MySQL generates temporary tables for some complicated joins. In these cases, MySQL assumes that the data values fit within the column display width.

All integer types can have an optional (nonstandard) attribute UNSIGNED. Unsigned type can be used to permit only nonnegative numbers in a column or when you need a larger upper numeric range for the column. For example, if an INT column is UNSIGNED, the size of the column's range is the same but its endpoints shift from -2147483648 and 2147483647 up to 0 and 4294967295.

Floating-point and fixed-point types also can be UNSIGNED. As with integer types, this attribute prevents negative values from being stored in the column. Unlike the integer types, the upper range of column values remains the same.

If you specify ZEROFILL for a numeric column, MySQL automatically adds the UNSIGNED attribute to
the column.

Integer or floating-point data types can have the additional attribute AUTO_INCREMENT. When you insert a value of NULL into an indexed AUTO_INCREMENT column, the column is set to the next sequence value. Typically this is value+1, where value is the largest value for the column currently in the table. (AUTO_INCREMENT sequences begin with 1.)

Storing 0 into an AUTO_INCREMENT column has the same effect as storing NULL, unless the NO_AUTO_VALUE_ON_ZERO SQL mode is enabled.

Inserting NULL to generate AUTO_INCREMENT values requires that the column be declared NOT NULL. If the column is declared NULL, inserting NULL stores a NULL. When you insert any other value into an AUTO_INCREMENT column, the column is set to that value and the sequence is reset so that the next
automatically generated value follows sequentially from the inserted value.

In MySQL 5.7, negative values for AUTO_INCREMENT columns are not supported.

### Out-of-Range and Overflow Handling

When MySQL stores a value in a numeric column that is outside the permissible range of the column data type, the result depends on the SQL mode in effect at the time:

• If strict SQL mode is enabled, MySQL rejects the out-of-range value with an error, and the insert fails, in accordance with the SQL standard.

• If no restrictive modes are enabled, MySQL clips the value to the appropriate endpoint of the column data type range and stores the resulting value instead. When an out-of-range value is assigned to an integer column, MySQL stores the value representing the corresponding endpoint of the column data type range.
When a floating-point or fixed-point column is assigned a value that exceeds the range implied by the specified (or default) precision and scale, MySQL stores the value representing the corresponding endpoint of that range.

Suppose that a table t1 has this definition:

```sql
CREATE TABLE t1 (i1 TINYINT, i2 TINYINT UNSIGNED);
```

With strict SQL mode enabled, an out of range error occurs:

```sql
mysql> SET sql_mode = 'TRADITIONAL';
mysql> INSERT INTO t1 (i1, i2) VALUES(256, 256);
ERROR 1264 (22003): Out of range value for column 'i1' at row 1
mysql> SELECT * FROM t1;
Empty set (0.00 sec)
```

With strict SQL mode not enabled, clipping with warnings occurs:

```sql
mysql> SET sql_mode = '';
mysql> INSERT INTO t1 (i1, i2) VALUES(256, 256);
mysql> SHOW WARNINGS;
+---------+------+---------------------------------------------+
| Level   | Code | 									   Message |
+---------+------+---------------------------------------------+
| Warning | 1264 | Out of range value for column 'i1' at row 1 |
| Warning | 1264 | Out of range value for column 'i2' at row 1 |
+---------+------+---------------------------------------------+
mysql> SELECT * FROM t1;
+------+------+
|   i1 |   i2 |
+------+------+
|  127 |  255 |
+------+------+
```

When strict SQL mode is not enabled, column-assignment conversions that occur due to clipping are reported as warnings for ALTER TABLE, LOAD DATA INFILE, UPDATE, and multiple-row INSERT statements. In strict mode, these statements fail, and some or all the values are not inserted or changed, depending on whether the table is a transactional table and other factors.

Overflow during numeric expression evaluation results in an error. For example, the largest signed BIGINT value is 9223372036854775807, so the following expression produces an error:

```sql
mysql> SELECT 9223372036854775807 + 1;
ERROR 1690 (22003): BIGINT value is out of range in '(9223372036854775807 + 1)'
```

To enable the operation to succeed in this case, convert the value to unsigned;

```sql
mysql> SELECT CAST(9223372036854775807 AS UNSIGNED) + 1;
+-------------------------------------------+
| CAST(9223372036854775807 AS UNSIGNED) + 1 |
+-------------------------------------------+
| 						9223372036854775808 |
+-------------------------------------------+
```

Whether overflow occurs depends on the range of the operands, so another way to handle the preceding expression is to use exact-value arithmetic because DECIMAL values have a larger range than integers:

```sql
mysql> SELECT 9223372036854775807.0 + 1;
+---------------------------+
| 9223372036854775807.0 + 1 |
+---------------------------+
|	  9223372036854775808.0 |
+---------------------------+
```

Subtraction between integer values, where one is of type UNSIGNED, produces an unsigned result by default. If the result would otherwise have been negative, an error results:

```sql
mysql> SET sql_mode = '';
Query OK, 0 rows affected (0.00 sec)
mysql> SELECT CAST(0 AS UNSIGNED) - 1;
ERROR 1690 (22003): BIGINT UNSIGNED value is out of range in '(cast(0 as unsigned) - 1)'
```

If the NO_UNSIGNED_SUBTRACTION SQL mode is enabled, the result is negative:

```sql
mysql> SET sql_mode = 'NO_UNSIGNED_SUBTRACTION';
mysql> SELECT CAST(0 AS UNSIGNED) - 1;
+-------------------------+
| CAST(0 AS UNSIGNED) - 1 |
+-------------------------+
| 					   -1 |
+-------------------------+
```

If the result of such an operation is used to update an UNSIGNED integer column, the result is clipped to the maximum value for the column type, or clipped to 0 if NO_UNSIGNED_SUBTRACTION is enabled. If strict SQL mode is enabled, an error occurs and the column remains unchanged.

## Date and Time Types

The date and time types for representing temporal values are DATE, TIME, DATETIME, TIMESTAMP, and YEAR. Each temporal type has a range of valid values, as well as a “zero” value that may be used when you specify an invalid value that MySQL cannot represent. The TIMESTAMP type has special automatic updating behavior, described later.

Keep in mind these general considerations when working with date and time types:

• MySQL retrieves values for a given date or time type in a standard output format, but it attempts to interpret a variety of formats for input values that you supply (for example, when you specify a value
to be assigned to or compared to a date or time type). It is expected that you supply valid values. Unpredictable results may occur if you use values in other formats.

• Although MySQL tries to interpret values in several formats, date parts must always be given in year-month-day order (for example, '98-09-04'), rather than in the month-day-year or day-month-year orders commonly used elsewhere (for example, '09-04-98', '04-09-98').

• Dates containing two-digit year values are ambiguous because the century is unknown. MySQL interprets two-digit year values using these rules:

	• Year values in the range 70-99 are converted to 1970-1999.
	• Year values in the range 00-69 are converted to 2000-2069.

• MySQL automatically converts a date or time value to a number if the value is used in a numeric context and vice versa.

• By default, when MySQL encounters a value for a date or time type that is out of range or otherwise invalid for the type, it converts the value to the “zero” value for that type. The exception is that out-of range
TIME values are clipped to the appropriate endpoint of the TIME range.

• By setting the SQL mode to the appropriate value, you can specify more exactly what kind of dates you want MySQL to support. You can get MySQL to accept certain dates, such as '2009-11-31', by enabling the ALLOW_INVALID_DATES SQL mode. This is useful when you want to store a “possibly wrong” value which the user has specified (for example, in a web form) in the database for future processing. Under this mode, MySQL verifies only that the month is in the range from 1 to 12 and that the day is in the range from 1 to 31.

• MySQL permits you to store dates where the day or month and day are zero in a DATE or DATETIME column. This is useful for applications that need to store birthdates for which you may not know the exact date. In this case, you simply store the date as '2009-00-00' or '2009-01-00'. If you store dates such as these, you should not expect to get correct results for functions such as DATE_SUB() or DATE_ADD() that require complete dates. To disallow zero month or day parts in dates, enable the NO_ZERO_IN_DATE mode.

• MySQL permits you to store a “zero” value of '0000-00-00' as a “dummy date.” This is in some cases more convenient than using NULL values, and uses less data and index space. To disallow '0000-00-00', enable the NO_ZERO_DATE mode.

• “Zero” date or time values used through Connector/ODBC are converted automatically to NULL because ODBC cannot handle such values.

The following table shows the format of the “zero” value for each type. The “zero” values are special, but you can store or refer to them explicitly using the values shown in the table. You can also do this using the values '0' or 0, which are easier to write. For temporal types that include a date part (DATE, DATETIME, and TIMESTAMP), use of these values produces warnings if the NO_ZERO_DATE SQL mode is enabled.

| Data Type | “Zero” Value          |
| --------- | --------------------- |
| DATE      | '0000-00-00'          |
| TIME      | '00:00:00'            |
| DATETIME  | '0000-00-00 00:00:00' |
| TIMESTAMP | '0000-00-00 00:00:00' |
| YEAR      | 0000                  |

### The DATE, DATETIME, and TIMESTAMP Types

The DATE, DATETIME, and TIMESTAMP types are related. This section describes their characteristics,
how they are similar, and how they differ. MySQL recognizes DATE, DATETIME, and TIMESTAMP
values in several formats. For the DATE and DATETIME range descriptions, “supported” means that although earlier values might work, there is no guarantee.

#### Date and Time Literals

Date and time values can be represented in several formats, such as quoted strings or as numbers, depending on the exact type of the value and other factors. For example, in contexts where MySQL expects a date, it interprets any of '2015-07-21', '20150721', and 20150721 as a date.

**Standard SQL and ODBC Date and Time Literals.** 

Standard SQL permits temporal literals to be specified using a type keyword and a string. The space between the keyword and string is optional.

```sql
DATE 'str'
TIME 'str'
TIMESTAMP 'str'
```

MySQL recognizes those constructions and also the corresponding ODBC syntax:

```sql
{ d 'str' }
{ t 'str' }
{ ts 'str' }
```

MySQL uses the type keyword and these constructions produce DATE, TIME, and DATETIME values, respectively, including a trailing fractional seconds part if specified. The TIMESTAMP syntax produces a DATETIME value in MySQL because DATETIME has a range that more closely corresponds to the standard SQL TIMESTAMP type, which has a year range from 0001 to 9999. (The MySQL TIMESTAMP year range is 1970 to 2038.)

**String and Numeric Literals in Date and Time Context.** 

MySQL recognizes DATE values in these formats:

• As a string in either 'YYYY-MM-DD' or 'YY-MM-DD' format. A “relaxed” syntax is permitted: Any punctuation character may be used as the delimiter between date parts. For example, '2012-12-31', '2012/12/31', '2012^12^31', and '2012@12@31' are equivalent.

• As a string with no delimiters in either 'YYYYMMDD' or 'YYMMDD' format, provided that the string makes sense as a date. For example, '20070523' and '070523' are interpreted as '2007-05-23', but '071332' is illegal (it has nonsensical month and day parts) and becomes '0000-00-00'.

• As a number in either YYYYMMDD or YYMMDD format, provided that the number makes sense as a date. For example, 19830905 and 830905 are interpreted as '1983-09-05'. 

MySQL recognizes DATETIME and TIMESTAMP values in these formats:

• As a string in either 'YYYY-MM-DD HH:MM:SS' or 'YY-MM-DD HH:MM:SS' format. A “relaxed” syntax is permitted here, too: Any punctuation character may be used as the delimiter between date parts or time parts. For example, '2012-12-31 11:30:45', '2012^12^31 11+30+45', '2012/12/31 11*30*45', and '2012@12@31 11^30^45' are equivalent.

The only delimiter recognized between a date and time part and a fractional seconds part is the decimal point.

The date and time parts can be separated by T rather than a space. For example, '2012-12-31 11:30:45' '2012-12-31T11:30:45' are equivalent.

• As a string with no delimiters in either 'YYYYMMDDHHMMSS' or 'YYMMDDHHMMSS' format, provided that the string makes sense as a date. For example, '20070523091528' and '070523091528' are interpreted as '2007-05-23 09:15:28', but '071122129015' is illegal (it has a nonsensical minute part) and becomes '0000-00-00 00:00:00'.

• As a number in either YYYYMMDDHHMMSS or YYMMDDHHMMSS format, provided that the number makes sense as a date. For example, 19830905132800 and 830905132800 are interpreted as '1983-09-05 13:28:00'.

A DATETIME or TIMESTAMP value can include a trailing fractional seconds part in up to microseconds (6 digits) precision. The fractional part should always be separated from the rest of the time by a decimal point; no other fractional seconds delimiter is recognized.

Dates containing two-digit year values are ambiguous because the century is unknown. MySQL interprets two-digit year values using these rules:

• Year values in the range 70-99 are converted to 1970-1999.
• Year values in the range 00-69 are converted to 2000-2069.

For values specified as strings that include date part delimiters, it is unnecessary to specify two digits for month or day values that are less than 10. '2015-6-9' is the same as '2015-06-09'. Similarly, for values specified as strings that include time part delimiters, it is unnecessary to specify two digits for hour, minute, or second values that are less than 10. '2015-10-30 1:2:3' is the same as '2015-10-30 01:02:03'.

Values specified as numbers should be 6, 8, 12, or 14 digits long. If a number is 8 or 14 digits long, it is assumed to be in YYYYMMDD or YYYYMMDDHHMMSS format and that the year is given by the first 4 digits. If the number is 6 or 12 digits long, it is assumed to be in YYMMDD or YYMMDDHHMMSS format and that the year is given by the first 2 digits. Numbers that are not one of these lengths are interpreted as though padded with leading zeros to the closest length.

Values specified as nondelimited strings are interpreted according their length. For a string 8 or 14 characters long, the year is assumed to be given by the first 4 characters. Otherwise, the year is assumed to be given by the first 2 characters. The string is interpreted from left to right to find year, month, day, hour, minute, and second values, for as many parts as are present in the string. This means you should not use strings that have fewer than 6 characters. For example, if you specify '9903', thinking that represents March, 1999, MySQL converts it to the “zero” date value. This occurs because the year and month values are 99 and 03, but the day part is completely missing. However, you can explicitly specify a value of zero to represent missing month or day parts. For example, to insert the value '1999-03-00', use '990300'.

MySQL recognizes TIME values in these formats:

• As a string in 'D HH:MM:SS' format. You can also use one of the following “relaxed” syntaxes: 'HH:MM:SS', 'HH:MM', 'D HH:MM', 'D HH', or 'SS'. Here D represents days and can have a value from 0 to 34.

• As a string with no delimiters in 'HHMMSS' format, provided that it makes sense as a time. For example, '101112' is understood as '10:11:12', but '109712' is illegal (it has a nonsensical minute part) and becomes '00:00:00'.

• As a number in HHMMSS format, provided that it makes sense as a time. For example, 101112 is understood as '10:11:12'. The following alternative formats are also understood: SS, MMSS, or HHMMSS.
A trailing fractional seconds part is recognized in the 'D HH:MM:SS.fraction', 'HH:MM:SS.fraction', 'HHMMSS.fraction', and HHMMSS.fraction time formats, where fraction is the fractional part in up to microseconds (6 digits) precision. The fractional part should always be separated from the rest of the time by a decimal point; no other fractional seconds delimiter is recognized.

For TIME values specified as strings that include a time part delimiter, it is unnecessary to specify two digits for hours, minutes, or seconds values that are less than 10. '8:3:2' is the same as '08:03:02'.v

The DATE type is used for values with a date part but no time part. MySQL retrieves and displays DATE values in 'YYYY-MM-DD' format. The supported range is '1000-01-01' to '9999-12-31'.

The DATETIME type is used for values that contain both date and time parts. MySQL retrieves and displays DATETIME values in 'YYYY-MM-DD HH:MM:SS' format. The supported range is '1000-01-01 00:00:00' to '9999-12-31 23:59:59'.

The TIMESTAMP data type is used for values that contain both date and time parts. TIMESTAMP has a
range of '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.

A DATETIME or TIMESTAMP value can include a trailing fractional seconds part in up to microseconds (6 digits) precision. In particular, any fractional part in a value inserted into a DATETIME or TIMESTAMP column is stored rather than discarded. With the fractional part included, the format for these values is 'YYYY-MM-DD HH:MM:SS[.fraction]', the range for DATETIME values is '1000-01-01 00:00:00.000000' to '9999-12-31 23:59:59.999999', and the range for TIMESTAMP values is '1970-01-01 00:00:01.000000' to '2038-01-19 03:14:07.999999'. The fractional part should always be separated from the rest of the time by a decimal point; no other fractional seconds delimiter is recognized.

The TIMESTAMP and DATETIME data types offer automatic initialization and updating to the current date and time.

MySQL converts TIMESTAMP values from the current time zone to UTC for storage, and back from UTC to the current time zone for retrieval. (This does not occur for other types such as DATETIME.)

By default, the current time zone for each connection is the server's time. The time zone can be set on a per-connection basis. As long as the time zone setting remains constant, you get back the same value you store. If you store a TIMESTAMP value, and then change the time zone and retrieve the value, the retrieved value is different from the value you stored. This occurs because the same time zone was not used for conversion in both directions. The current time zone is available as the value of the time_zone system variable.

Invalid DATE, DATETIME, or TIMESTAMP values are converted to the “zero” value of the appropriate type ('0000-00-00' or '0000-00-00 00:00:00').

Be aware of certain properties of date value interpretation in MySQL:

• MySQL permits a “relaxed” format for values specified as strings, in which any punctuation character may be used as the delimiter between date parts or time parts. In some cases, this syntax can be deceiving. For example, a value such as '10:11:12' might look like a time value because of the :, but is interpreted as the year '2010-11-12' if used in a date context. The value '10:45:15' is converted to '0000-00-00' because '45' is not a valid month.

The only delimiter recognized between a date and time part and a fractional seconds part is the decimal point.

• The server requires that month and day values be valid, and not merely in the range 1 to 12 and 1 to 31, respectively. With strict mode disabled, invalid dates such as '2004-04-31' are converted to '0000-00-00' and a warning is generated. With strict mode enabled, invalid dates generate an error. To permit such dates, enable ALLOW_INVALID_DATES.

• MySQL does not accept TIMESTAMP values that include a zero in the day or month column or values that are not a valid date. The sole exception to this rule is the special “zero” value '0000-00-00 00:00:00'.

• Dates containing two-digit year values are ambiguous because the century is unknown. MySQL interprets two-digit year values using these rules:

	• Year values in the range 00-69 are converted to 2000-2069.
	• Year values in the range 70-99 are converted to 1970-1999.

> **Note**
> The MySQL server can be run with the MAXDB SQL mode enabled. In this case, TIMESTAMP is identical with DATETIME. If this mode is enabled at the time that a table is created, TIMESTAMP columns are created as DATETIME columns. As a result, such columns use DATETIME display format, have the same range of values, and there is no automatic initialization or updating to the current date and time.

### The TIME Type

MySQL retrieves and displays TIME values in 'HH:MM:SS' format (or 'HHH:MM:SS' format for large hours values). TIME values may range from '-838:59:59' to '838:59:59'. The hours part may be so large because the TIME type can be used not only to represent a time of day (which must be less than 24 hours), but also elapsed time or a time interval between two events (which may be much greater than 24 hours, or even negative).

MySQL recognizes TIME values in several formats, some of which can include a trailing fractional seconds part in up to microseconds (6 digits) precision.

In particular, any fractional part in a value inserted into a TIME column is stored rather than discarded. With the fractional part included, the range for TIME values is '-838:59:59.000000' to '838:59:59.000000'.

Be careful about assigning abbreviated values to a TIME column. MySQL interprets abbreviated TIME values with colons as time of the day. That is, '11:12' means '11:12:00', not '00:11:12'.

MySQL interprets abbreviated values without colons using the assumption that the two rightmost digits represent seconds (that is, as elapsed time rather than as time of day). For example, you might think of '1112' and 1112 as meaning '11:12:00' (12 minutes after 11 o'clock), but MySQL interprets them as '00:11:12' (11 minutes, 12 seconds). Similarly, '12' and 12 are interpreted as '00:00:12'.

The only delimiter recognized between a time part and a fractional seconds part is the decimal point.

By default, values that lie outside the TIME range but are otherwise valid are clipped to the closest endpoint of the range. For example, '-850:00:00' and '850:00:00' are converted to '-838:59:59' and '838:59:59'. Invalid TIME values are converted to '00:00:00'. Note that because '00:00:00' is itself a valid TIME value, there is no way to tell, from a value of '00:00:00' stored in a table, whether the original value was specified as '00:00:00' or whether it was invalid.

### The YEAR Type

The YEAR type is a 1-byte type used to represent year values. It can be declared as YEAR or YEAR(4) and has a display width of four characters.

> **Note**
> The YEAR(2) data type is deprecated and support for it is removed in MySQL 5.7.5.

MySQL displays YEAR values in YYYY format, with a range of 1901 to 2155, or 0000.

You can specify input YEAR values in a variety of formats:

• As a 4-digit number in the range 1901 to 2155.

• As a 4-digit string in the range '1901' to '2155'.

• As a 1- or 2-digit number in the range 1 to 99. MySQL converts values in the ranges 1 to 69 and 70 to 99 to YEAR values in the ranges 2001 to 2069 and 1970 to 1999.

• As a 1- or 2-digit string in the range '0' to '99'. MySQL converts values in the ranges '0' to '69' and '70' to '99' to YEAR values in the ranges 2000 to 2069 and 1970 to 1999.

• The result of inserting a numeric 0 has a display value of 0000 and an internal value of 0000. To insert zero and have it be interpreted as 2000, specify it as a string '0' or '00'.

• As the result of a function that returns a value that is acceptable in a YEAR context, such as NOW(). MySQL converts invalid YEAR values to 0000.

### Automatic Initialization and Updating for TIMESTAMP and DATETIME

TIMESTAMP and DATETIME columns can be automatically initializated and updated to the current date and time (that is, the current timestamp).

For any TIMESTAMP or DATETIME column in a table, you can assign the current timestamp as the default value, the auto-update value, or both:

• An auto-initialized column is set to the current timestamp for inserted rows that specify no value for the column.

• An auto-updated column is automatically updated to the current timestamp when the value of any other column in the row is changed from its current value. An auto-updated column remains unchanged if all other columns are set to their current values. To prevent an auto-updated column from updating when other columns change, explicitly set it to its current value. To update an autoupdated column even when other columns do not change, explicitly set it to the value it should have (for example, set it to CURRENT_TIMESTAMP).

In addition, if the explicit_defaults_for_timestamp system variable is disabled, you can initialize or update any TIMESTAMP (but not DATETIME) column to the current date and time by assigning it a NULL value, unless it has been defined with the NULL attribute to permit NULL values.

To specify automatic properties, use the DEFAULT CURRENT_TIMESTAMP and ON UPDATE CURRENT_TIMESTAMP clauses in column definitions. The order of the clauses does not matter. If both are present in a column definition, either can occur first. Any of the synonyms for CURRENT_TIMESTAMP have the same meaning as CURRENT_TIMESTAMP. These are CURRENT_TIMESTAMP(), NOW(), LOCALTIME, LOCALTIME(), LOCALTIMESTAMP, and LOCALTIMESTAMP().

Use of DEFAULT CURRENT_TIMESTAMP and ON UPDATE CURRENT_TIMESTAMP is specific to TIMESTAMP and DATETIME. The DEFAULT clause also can be used to specify a constant (nonautomatic) default value; for example, DEFAULT 0 or DEFAULT '2000-01-01 00:00:00'.

> **Note**
> The following examples use DEFAULT 0, a default that can produce warnings or errors depending on whether strict SQL mode or the NO_ZERO_DATE SQL mode is enabled. Be aware that the TRADITIONAL SQL mode includes strict mode and NO_ZERO_DATE.

TIMESTAMP or DATETIME column definitions can specify the current timestamp for both the default and auto-update values, for one but not the other, or for neither. Different columns can have different combinations of automatic properties. The following rules describe the possibilities:

• With both DEFAULT CURRENT_TIMESTAMP and ON UPDATE CURRENT_TIMESTAMP, the column has the current timestamp for its default value and is automatically updated to the current timestamp.

```sql
CREATE TABLE t1 (
	ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	dt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

• With a DEFAULT clause but no ON UPDATE CURRENT_TIMESTAMP clause, the column has the given default value and is not automatically updated to the current timestamp.

The default depends on whether the DEFAULT clause specifies CURRENT_TIMESTAMP or a constant value. With CURRENT_TIMESTAMP, the default is the current timestamp.

```sql
CREATE TABLE t1 (
	ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	dt DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

With a constant, the default is the given value. In this case, the column has no automatic properties at all.

```sql
CREATE TABLE t1 (
	ts TIMESTAMP DEFAULT 0,
	dt DATETIME DEFAULT 0
);
```

• With an ON UPDATE CURRENT_TIMESTAMP clause and a constant DEFAULT clause, the column is automatically updated to the current timestamp and has the given constant default value.

```sql
CREATE TABLE t1 (
	ts TIMESTAMP DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP,
	dt DATETIME DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP
);
```

• With an ON UPDATE CURRENT_TIMESTAMP clause but no DEFAULT clause, the column is automatically updated to the current timestamp but does not have the current timestamp for its default value.

The default in this case is type dependent. TIMESTAMP has a default of 0 unless defined with the NULL attribute, in which case the default is NULL.

```sql
CREATE TABLE t1 (
	ts1 TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- default 0
	ts2 TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP -- default NULL
);
```

DATETIME has a default of NULL unless defined with the NOT NULL attribute, in which case the default is 0.

```sql
CREATE TABLE t1 (
	dt1 DATETIME ON UPDATE CURRENT_TIMESTAMP, -- default NULL
	dt2 DATETIME NOT NULL ON UPDATE CURRENT_TIMESTAMP -- default 0
);
```

TIMESTAMP and DATETIME columns have no automatic properties unless they are specified explicitly, with this exception: If the explicit_defaults_for_timestamp system variable is disabled, the first TIMESTAMP column has both DEFAULT CURRENT_TIMESTAMP and ON UPDATE CURRENT_TIMESTAMP if neither is specified explicitly. To suppress automatic properties for the first TIMESTAMP column, use one of these strategies:

• Enable the explicit_defaults_for_timestamp system variable. In this case, the DEFAULT CURRENT_TIMESTAMP and ON UPDATE CURRENT_TIMESTAMP clauses that specify automatic initialization and updating are available, but are not assigned to any TIMESTAMP column unless explicitly included in the column definition.

• Alternatively, if explicit_defaults_for_timestamp is disabled, do either of the following:

 - Define the column with a DEFAULT clause that specifies a constant default value.
 - Specify the NULL attribute. This also causes the column to permit NULL values, which means that you cannot assign the current timestamp by setting the column to NULL. Assigning NULL sets the column to NULL, not the current timestamp. To assign the current timestamp, set the column to CURRENT_TIMESTAMP or a synonym such as NOW().

Consider these table definitions:

```sql
CREATE TABLE t1 (
	ts1 TIMESTAMP DEFAULT 0,
	ts2 TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE t2 (
	ts1 TIMESTAMP NULL,
	ts2 TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE t3 (
	ts1 TIMESTAMP NULL DEFAULT 0,
	ts2 TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

The tables have these properties:

• In each table definition, the first TIMESTAMP column has no automatic initialization or updating.

• The tables differ in how the ts1 column handles NULL values. For t1, ts1 is NOT NULL and assigning it a value of NULL sets it to the current timestamp. For t2 and t3, ts1 permits NULL and assigning it a value of NULL sets it to NULL.

• t2 and t3 differ in the default value for ts1. For t2, ts1 is defined to permit NULL, so the default is also NULL in the absence of an explicit DEFAULT clause. For t3, ts1 permits NULL but has an explicit default of 0.

If a TIMESTAMP or DATETIME column definition includes an explicit fractional seconds precision value anywhere, the same value must be used throughout the column definition. This is permitted:

```sql
CREATE TABLE t1 (
	ts TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);
```

This is not permitted:

```sql
CREATE TABLE t1 (
	ts TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(3)
);
```

**TIMESTAMP Initialization and the NULL Attribute**

If the explicit_defaults_for_timestamp system variable is disabled, TIMESTAMP columns by default are NOT NULL, cannot contain NULL values, and assigning NULL assigns the current timestamp. To permit a TIMESTAMP column to contain NULL, explicitly declare it with the NULL attribute. In this case, the default value also becomes NULL unless overridden with a DEFAULT clause that specifies a different default value. DEFAULT NULL can be used to explicitly specify NULL as the default value. (For a TIMESTAMP column not declared with the NULL attribute, DEFAULT NULL is invalid.) If a TIMESTAMP column permits NULL values, assigning NULL sets it to NULL, not to the current timestamp.

The following table contains several TIMESTAMP columns that permit NULL values:

```sql
CREATE TABLE t (
	ts1 TIMESTAMP NULL DEFAULT NULL,
	ts2 TIMESTAMP NULL DEFAULT 0,
	ts3 TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);
```

A TIMESTAMP column that permits NULL values does not take on the current timestamp at insert time except under one of the following conditions:

• Its default value is defined as CURRENT_TIMESTAMP and no value is specified for the column

• CURRENT_TIMESTAMP or any of its synonyms such as NOW() is explicitly inserted into the column In other words, a TIMESTAMP column defined to permit NULL values auto-initializes only if its definition includes DEFAULT CURRENT_TIMESTAMP:

```sql
CREATE TABLE t (ts TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP);
```

If the TIMESTAMP column permits NULL values but its definition does not include DEFAULT CURRENT_TIMESTAMP, you must explicitly insert a value corresponding to the current date and time.

Suppose that tables t1 and t2 have these definitions:

```sql
CREATE TABLE t1 (ts TIMESTAMP NULL DEFAULT '0000-00-00 00:00:00');
CREATE TABLE t2 (ts TIMESTAMP NULL DEFAULT NULL);
```

To set the TIMESTAMP column in either table to the current timestamp at insert time, explicitly assign it
that value. For example:

```sql
INSERT INTO t2 VALUES (CURRENT_TIMESTAMP);
INSERT INTO t1 VALUES (NOW());
```

If the explicit_defaults_for_timestamp system variable is enabled, TIMESTAMP columns permit NULL values only if declared with the NULL attribute. Also, TIMESTAMP columns do not permit assigning NULL to assign the current timestamp, whether declared with the NULL or NOT NULL attribute. To assign the current timestamp, set the column to CURRENT_TIMESTAMP or a synonym such as NOW().

### Fractional Seconds in Time Values

MySQL 5.7 has fractional seconds support for TIME, DATETIME, and TIMESTAMP values, with up to microseconds (6 digits) precision:

• To define a column that includes a fractional seconds part, use the syntax type_name(fsp), where type_name is TIME, DATETIME, or TIMESTAMP, and fsp is the fractional seconds precision. For example:

```sql
CREATE TABLE t1 (t TIME(3), dt DATETIME(6));
```

The fsp value, if given, must be in the range 0 to 6. A value of 0 signifies that there is no fractional part. If omitted, the default precision is 0. (This differs from the standard SQL default of 6, for compatibility with previous MySQL versions.)

• Inserting a TIME, DATE, or TIMESTAMP value with a fractional seconds part into a column of the same type but having fewer fractional digits results in rounding, as shown in this example:

```sql
mysql> CREATE TABLE fractest( c1 TIME(2), c2 DATETIME(2), c3 TIMESTAMP(2) );
Query OK, 0 rows affected (0.33 sec)

mysql> INSERT INTO fractest VALUES
 	-> ('17:51:04.777', '2014-09-08 17:51:04.777', '2014-09-08 17:51:04.777');
Query OK, 1 row affected (0.03 sec)

mysql> SELECT * FROM fractest;
+-------------+------------------------+------------------------+
|		   c1 | 					c2 | 					 c3 |
+-------------+------------------------+------------------------+
| 17:51:04.78 | 2014-09-08 17:51:04.78 | 2014-09-08 17:51:04.78 |
+-------------+------------------------+------------------------+
1 row in set (0.00 sec)
```

No warning or error is given when such rounding occurs. This behavior follows the SQL standard, and is not affected by the server's sql_mode setting.

• Functions that take temporal arguments accept values with fractional seconds. Return values from temporal functions include fractional seconds as appropriate. For example, NOW() with no argument returns the current date and time with no fractional part, but takes an optional argument from 0 to 6 to specify that the return value includes a fractional seconds part of that many digits.

• Syntax for temporal literals produces temporal values: DATE 'str', TIME 'str', and TIMESTAMP 'str', and the ODBC-syntax equivalents. The resulting value includes a trailing fractional seconds part if specified. Previously, the temporal type keyword was ignored and these constructs produced the string value.

### Conversion Between Date and Time Types

To some extent, you can convert a value from one temporal type to another. However, there may be some alteration of the value or loss of information. In all cases, conversion between temporal types is subject to the range of valid values for the resulting type. For example, although DATE, DATETIME, and TIMESTAMP values all can be specified using the same set of formats, the types do not all have the same range of values. TIMESTAMP values cannot be earlier than 1970 UTC or later than '2038-01-19 03:14:07' UTC. This means that a date such as '1968-01-01', while valid as a DATE or DATETIME value, is not valid as a TIMESTAMP value and is converted to 0.

Conversion of DATE values:

• Conversion to a DATETIME or TIMESTAMP value adds a time part of '00:00:00' because the DATE value contains no time information.

• Conversion to a TIME value is not useful; the result is '00:00:00'.

Conversion of DATETIME and TIMESTAMP values:

• Conversion to a DATE value takes fractional seconds into account and rounds the time part. For example, '1999-12-31 23:59:59.499' becomes '1999-12-31', whereas '1999-12-31 23:59:59.500' becomes '2000-01-01'.

• Conversion to a TIME value discards the date part because the TIME type contains no date information.

For conversion of TIME values to other temporal types, the value of CURRENT_DATE() is used for the date part. The TIME is interpreted as elapsed time (not time of day) and added to the date. This means that the date part of the result differs from the current date if the time value is outside the range from '00:00:00' to '23:59:59'.

Suppose that the current date is '2012-01-01'. TIME values of '12:00:00', '24:00:00', and '-12:00:00', when converted to DATETIME or TIMESTAMP values, result in '2012-01-01 12:00:00', '2012-01-02 00:00:00', and '2011-12-31 12:00:00', respectively.

Conversion of TIME to DATE is similar but discards the time part from the result: '2012-01-01', '2012-01-02', and '2011-12-31', respectively.

Explicit conversion can be used to override implicit conversion. For example, in comparison of DATE and DATETIME values, the DATE value is coerced to the DATETIME type by adding a time part of '00:00:00'. To perform the comparison by ignoring the time part of the DATETIME value instead, use the CAST() function in the following way:

```sql
date_col = CAST(datetime_col AS DATE)
```

Conversion of TIME and DATETIME values to numeric form (for example, by adding +0) depends on whether the value contains a fractional seconds part. TIME(N) or DATETIME(N) is converted to integer when N is 0 (or omitted) and to a DECIMAL value with N decimal digits when N is greater than 0:

```sql
mysql> SELECT CURTIME(), CURTIME()+0, CURTIME(3)+0;
+-----------+-------------+--------------+
| CURTIME() | CURTIME()+0 | CURTIME(3)+0 |
+-----------+-------------+--------------+
|  09:28:00 | 		92800 |	   92800.887 |
+-----------+-------------+--------------+

mysql> SELECT NOW(), NOW()+0, NOW(3)+0;
+---------------------+----------------+--------------------+
|			    NOW() |		   NOW()+0 | 		   NOW(3)+0 |
+---------------------+----------------+--------------------+
| 2012-08-15 09:28:00 | 20120815092800 | 20120815092800.889 |
+---------------------+----------------+--------------------+
```

## String Types

The string types are CHAR, VARCHAR, BINARY, VARBINARY, BLOB, TEXT, ENUM, and SET. 

### The CHAR and VARCHAR Types

The CHAR and VARCHAR types are similar, but **differ in the way they are stored and retrieved**. They also **differ in maximum length and in whether trailing spaces are retained**.

The CHAR and VARCHAR types are declared with a length that indicates the maximum number of characters you want to store. For example, CHAR(30) can hold up to 30 characters.

The length of a CHAR column is **fixed** to the length that you declare when you create the table. The length can be any value from 0 to 255. When CHAR values are stored, they are right-padded with spaces to the specified length. When CHAR values are retrieved, trailing spaces are removed unless the PAD_CHAR_TO_FULL_LENGTH SQL mode is enabled.

Values in VARCHAR columns are variable-length strings. The length can be specified as a value from 0 to 65,535. The effective maximum length of a VARCHAR is subject to the maximum row size (65,535 bytes, which is shared among all columns) and the character set used.

In contrast to CHAR, VARCHAR values are stored as a 1-byte or 2-byte length prefix plus data. The length prefix indicates the number of bytes in the value. A column uses one length byte if values require no more than 255 bytes, two length bytes if values may require more than 255 bytes.

If strict SQL mode is not enabled and you assign a value to a CHAR or VARCHAR column that exceeds the column's maximum length, the value is truncated to fit and a warning is generated. For truncation of nonspace characters, you can cause an error to occur (rather than a warning) and suppress insertion of the value by using strict SQL mode.

For VARCHAR columns, trailing spaces in excess of the column length are truncated prior to insertion and a warning is generated, regardless of the SQL mode in use. For CHAR columns, truncation of excess trailing spaces from inserted values is performed silently regardless of the SQL mode.

VARCHAR values are not padded when they are stored. Trailing spaces are retained when values are stored and retrieved, in conformance with standard SQL.

The following table illustrates the differences between CHAR and VARCHAR by showing the result of storing various string values into CHAR(4) and VARCHAR(4) columns (assuming that the column uses a single-byte character set such as latin1).

| Value      | CHAR(4) | Storage Required | VARCHAR(4) | Storage Required |
| ---------- | ------- | ---------------- | ---------- | ---------------- |
| ''         | '    '  | 4 bytes          | ''         | 1 byte           |
| 'ab'       | 'ab  '  | 4 bytes          | 'ab'       | 3 bytes          |
| 'abcd'     | 'abcd'  | 4 bytes          | 'abcd'     | 5 bytes          |
| 'abcdefgh' | 'abcd'  | 4 bytes          | 'abcd'     | 5 bytes          |

The values shown as stored in the last row of the table apply only when not using strict mode; if MySQL is running in strict mode, values that exceed the column length are not stored, and an error results.

InnoDB encodes fixed-length fields greater than or equal to 768 bytes in length as variable-length fields, which can be stored off-page. For example, a CHAR(255) column can exceed 768 bytes if the maximum byte length of the character set is greater than 3, as it is with utf8mb4.

If a given value is stored into the CHAR(4) and VARCHAR(4) columns, the values retrieved from the columns are not always the same because trailing spaces are removed from CHAR columns upon retrieval. The following example illustrates this difference:

```sql
mysql> CREATE TABLE vc (v VARCHAR(4), c CHAR(4));
Query OK, 0 rows affected (0.34 sec)

mysql>  INSERT INTO vc VALUES ('ab ', 'ab ');
Query OK, 1 row affected (0.12 sec)

mysql>  SELECT CONCAT('(', v, ')'), CONCAT('(', c, ')') FROM vc;
+---------------------+---------------------+
| CONCAT('(', v, ')') | CONCAT('(', c, ')') |
+---------------------+---------------------+
| (ab )               | (ab)                |
+---------------------+---------------------+
1 row in set (0.04 sec)
```

Values in CHAR and VARCHAR columns are sorted and compared according to the character set collation assigned to the column.

All MySQL collations are of type PAD SPACE. This means that all CHAR, VARCHAR, and TEXT values are compared without regard to any trailing spaces. “Comparison” in this context does not include the LIKE pattern-matching operator, for which trailing spaces are significant. For example:

```sql
mysql>  CREATE TABLE names (myname CHAR(10));
Query OK, 0 rows affected (0.45 sec)

mysql>  INSERT INTO names VALUES ('Monty');
Query OK, 1 row affected (0.07 sec)

mysql>  SELECT myname = 'Monty', myname = 'Monty ' FROM names;
+------------------+-------------------+
| myname = 'Monty' | myname = 'Monty ' |
+------------------+-------------------+
|                1 |                 1 |
+------------------+-------------------+
1 row in set (0.00 sec)

mysql>  SELECT myname LIKE 'Monty', myname LIKE 'Monty ' FROM names;
+---------------------+----------------------+
| myname LIKE 'Monty' | myname LIKE 'Monty ' |
+---------------------+----------------------+
|                   1 |                    0 |
+---------------------+----------------------+
1 row in set (0.00 sec)
```

This is true for all MySQL versions, and is not affected by the server SQL mode.

For those cases where trailing pad characters are stripped or comparisons ignore them, if a column has an index that requires unique values, inserting into the column values that differ only in number of trailing pad characters will result in a duplicate-key error. For example, if a table contains 'a', an attempt to store 'a ' causes a duplicate-key error.

### The BINARY and VARBINARY Types

The BINARY and VARBINARY types are similar to CHAR and VARCHAR, except that they contain binary strings rather than nonbinary strings. That is, **they contain byte strings rather than character strings.** This means they have the binary character set and collation, and comparison and sorting are based on the numeric values of the bytes in the values.

The permissible maximum length is the same for BINARY and VARBINARY as it is for CHAR and VARCHAR, except that the length for BINARY and VARBINARY is a length in bytes rather than in characters.

The BINARY and VARBINARY data types are distinct from the CHAR BINARY and VARCHAR BINARY data types. For the latter types, the BINARY attribute does not cause the column to be treated as a binary string column. Instead, it causes the binary (_bin) collation for the column character set to be used, and the column itself contains nonbinary character strings rather than binary byte strings.

For example, CHAR(5) BINARY is treated as CHAR(5) CHARACTER SET latin1 COLLATE latin1_bin, assuming that the default character set is latin1. This differs from BINARY(5), which stores 5-bytes binary strings that have the binary character set and collation.

If strict SQL mode is not enabled and you assign a value to a BINARY or VARBINARY column that exceeds the column's maximum length, the value is truncated to fit and a warning is generated. For cases of truncation, you can cause an error to occur (rather than a warning) and suppress insertion of the value by using strict SQL mode.

When BINARY values are stored, they are right-padded with the pad value to the specified length. The pad value is 0x00 (the zero byte). Values are right-padded with 0x00 on insert, and no trailing bytes are removed on select. All bytes are significant in comparisons, including ORDER BY and DISTINCT operations. 0x00 bytes and spaces are different in comparisons, with 0x00 < space.

Example: For a BINARY(3) column, 'a ' becomes 'a \0' when inserted. 'a\0' becomes 'a\0\0' when inserted. Both inserted values remain unchanged when selected.

For VARBINARY, there is no padding on insert and no bytes are stripped on select. All bytes are significant in comparisons, including ORDER BY and DISTINCT operations. 0x00 bytes and spaces are different in comparisons, with 0x00 < space.

For those cases where trailing pad bytes are stripped or comparisons ignore them, if a column has an index that requires unique values, inserting into the column values that differ only in number of trailing pad bytes will result in a duplicate-key error. For example, if a table contains 'a', an attempt to store 'a\0' causes a duplicate-key error.

You should consider the preceding padding and stripping characteristics carefully if you plan to use the BINARY data type for storing binary data and you require that the value retrieved be exactly the same as the value stored. The following example illustrates how 0x00-padding of BINARY values affects column value comparisons:

```sql
mysql>  CREATE TABLE t (c BINARY(3));
Query OK, 0 rows affected (0.34 sec)

mysql>  INSERT INTO t SET c = 'a';
Query OK, 1 row affected (0.11 sec)

mysql>  SELECT HEX(c), c = 'a', c = 'a\0\0' from t;
+--------+---------+-------------+
| HEX(c) | c = 'a' | c = 'a\0\0' |
+--------+---------+-------------+
| 610000 |       0 |           1 |
+--------+---------+-------------+
1 row in set (0.00 sec)
```

If the value retrieved must be the same as the value specified for storage with no padding, it might be preferable to use VARBINARY or one of the BLOB data types instead.

### The BLOB and TEXT Types

A BLOB is a binary large object that can hold a variable amount of data. The four BLOB types are TINYBLOB, BLOB, MEDIUMBLOB, and LONGBLOB. These differ only in the maximum length of the values they can hold. The four TEXT types are TINYTEXT, TEXT, MEDIUMTEXT, and LONGTEXT. These correspond to the four BLOB types and have the same maximum lengths and storage requirements.

BLOB values are treated as binary strings (byte strings). They have the binary character set and collation, and comparison and sorting are based on the numeric values of the bytes in column values. TEXT values are treated as nonbinary strings (character strings). They have a character set other than binary, and values are sorted and compared based on the collation of the character set.

If strict SQL mode is not enabled and you assign a value to a BLOB or TEXT column that exceeds the column's maximum length, the value is truncated to fit and a warning is generated. For truncation of nonspace characters, you can cause an error to occur (rather than a warning) and suppress insertion of the value by using strict SQL mode.

Truncation of excess trailing spaces from values to be inserted into TEXT columns always generates a
warning, regardless of the SQL mode.

For TEXT and BLOB columns, there is no padding on insert and no bytes are stripped on select.

If a TEXT column is indexed, index entry comparisons are space-padded at the end. This means that, if the index requires unique values, duplicate-key errors will occur for values that differ only in the number of trailing spaces. For example, if a table contains 'a', an attempt to store 'a ' causes a duplicatekey error. This is not true for BLOB columns.

In most respects, you can regard a BLOB column as a VARBINARY column that can be as large as you like. Similarly, you can regard a TEXT column as a VARCHAR column. BLOB and TEXT differ from VARBINARY and VARCHAR in the following ways:

• For indexes on BLOB and TEXT columns, you must specify an index prefix length. For CHAR and VARCHAR, a prefix length is optional.

• BLOB and TEXT columns cannot have DEFAULT values.

If you use the BINARY attribute with a TEXT data type, the column is assigned the binary (_bin) collation of the column character set.

LONG and LONG VARCHAR map to the MEDIUMTEXT data type. This is a compatibility feature.

MySQL Connector/ODBC defines BLOB values as LONGVARBINARY and TEXT values as LONGVARCHAR.

Because BLOB and TEXT values can be extremely long, you might encounter some constraints in using
them:

• Only the first max_sort_length bytes of the column are used when sorting. The default value of max_sort_length is 1024. You can make more bytes significant in sorting or grouping by increasing the value of max_sort_length at server startup or runtime. Any client can change the value of its session max_sort_length variable:

```sql
mysql> SET max_sort_length = 2000;
mysql> SELECT id, comment FROM t ORDER BY comment;
```

• Instances of BLOB or TEXT columns in the result of a query that is processed using a temporary table causes the server to use a table on disk rather than in memory because the MEMORY storage engine does not support those data types. Use of disk incurs a performance penalty, so include BLOB or TEXT columns in the query result only if they are really needed. For example, avoid using SELECT *, which selects all columns.

• The maximum size of a BLOB or TEXT object is determined by its type, but the largest value you actually can transmit between the client and server is determined by the amount of available memory and the size of the communications buffers. You can change the message buffer size by changing the value of the max_allowed_packet variable, but you must do so for both the server and your client program. For example, both mysql and mysqldump enable you to change the client-side max_allowed_packet value. You may also want to compare the packet sizes and the size of the data objects you are storing with the storage requirements.

**Each BLOB or TEXT value is represented internally by a separately allocated object.** This is in contrast to all other data types, for which storage is allocated once per column when the table is opened.

In some cases, it may be desirable to store binary data such as media files in BLOB or TEXT columns. You may find MySQL's string handling functions useful for working with such data. For security and other reasons, it is usually preferable to do so using application code rather than giving application users the FILE privilege.

### The ENUM Type

An ENUM is a string object with a value chosen from a list of permitted values that are enumerated explicitly in the column specification at table creation time. It has these advantages:

• Compact data storage in situations where a column has a limited set of possible values. The strings you specify as input values are automatically encoded as numbers.

• Readable queries and output. The numbers are translated back to the corresponding strings in query results.

**Creating and Using ENUM Columns**

An enumeration value must be a quoted string literal. For example, you can create a table with an ENUM column like this:

```sql
mysql> CREATE TABLE shirts (
    ->  name VARCHAR(40),
    ->  size ENUM('x-small', 'small', 'medium', 'large', 'x-large')
    -> );
Query OK, 0 rows affected (0.57 sec)

mysql> INSERT INTO shirts (name, size) 
	-> VALUES ('dress shirt','large'), ('t-shirt','medium'), ('polo shirt','small');
Query OK, 3 rows affected (0.08 sec)
Records: 3  Duplicates: 0  Warnings: 0

mysql> SELECT name, size FROM shirts WHERE size = 'medium';
+---------+--------+
| name    | size   |
+---------+--------+
| t-shirt | medium |
+---------+--------+
1 row in set (0.04 sec)

mysql> UPDATE shirts SET size = 'small' WHERE size = 'large';
Query OK, 1 row affected (0.11 sec)
Rows matched: 1  Changed: 1  Warnings: 0

mysql> COMMIT;
Query OK, 0 rows affected (0.00 sec)
```

Inserting 1 million rows into this table with a value of 'medium' would require 1 million bytes of storage, as opposed to 6 million bytes if you stored the actual string 'medium' in a VARCHAR column.

**Index Values for Enumeration Literals**

Each enumeration value has an index:

• The elements listed in the column specification are assigned index numbers, beginning with 1.

• The index value of the empty string error value is 0. This means that you can use the following SELECT statement to find rows into which invalid ENUM values were assigned:

```sql
mysql> SELECT * FROM tbl_name WHERE enum_col=0;
```

• The index of the NULL value is NULL.

• The term “index” here refers to a position within the list of enumeration values. It has nothing to do
with table indexes.

For example, a column specified as ENUM('Mercury', 'Venus', 'Earth') can have any of the values shown here. The index of each value is also shown.

| Value     | Index |
| --------- | ----- |
| NULL      | NULL  |
| ''        | 0     |
| 'Mercury' | 1     |
| 'Venus'   | 2     |
| 'Earth'   | 3     |

An ENUM column can have a maximum of 65,535 distinct elements. (The practical limit is less than 3000.) A table can have no more than 255 unique element list definitions among its ENUM and SET columns considered as a group.

If you retrieve an ENUM value in a numeric context, the column value's index is returned. For example, you can retrieve numeric values from an ENUM column like this:

```sql
mysql> SELECT enum_col+0 FROM tbl_name;
```

Functions such as SUM() or AVG() that expect a numeric argument cast the argument to a number if necessary. For ENUM values, the index number is used in the calculation.

**Handling of Enumeration Literals**

Trailing spaces are automatically deleted from ENUM member values in the table definition when a table is created.

When retrieved, values stored into an ENUM column are displayed using the lettercase that was used in the column definition. Note that ENUM columns can be assigned a character set and collation. For binary or case-sensitive collations, lettercase is taken into account when assigning values to the column.

If you store a number into an ENUM column, the number is treated as the index into the possible values, and the value stored is the enumeration member with that index. (However, this does not work with LOAD DATA, which treats all input as strings.) If the numeric value is quoted, it is still interpreted as an index if there is no matching string in the list of enumeration values. For these reasons, it is not advisable to define an ENUM column with enumeration values that look like numbers, because this can easily become confusing. For example, the following column has enumeration members with string values of '0', '1', and '2', but numeric index values of 1, 2, and 3:

```sql
numbers ENUM('0','1','2')
```

If you store 2, it is interpreted as an index value, and becomes '1' (the value with index 2). If you store '2', it matches an enumeration value, so it is stored as '2'. If you store '3', it does not match any enumeration value, so it is treated as an index and becomes '2' (the value with index 3).

```sql
mysql> INSERT INTO t (numbers) VALUES(2),('2'),('3');
mysql> SELECT * FROM t;
+---------+
| numbers |
+---------+
| 		1 |
| 		2 |
| 		2 |
+---------+
```

To determine all possible values for an ENUM column, use SHOW COLUMNS FROM tbl_name LIKE 'enum_col' and parse the ENUM definition in the Type column of the output. In the C API, ENUM values are returned as strings.

**Empty or NULL Enumeration Values**

An enumeration value can also be the empty string ('') or NULL under certain circumstances:

• If you insert an invalid value into an ENUM (that is, a string not present in the list of permitted values), the empty string is inserted instead as a special error value. This string can be distinguished from a “normal” empty string by the fact that this string has the numeric value 0. See Index Values for Enumeration Literals for details about the numeric indexes for the enumeration values.

If strict SQL mode is enabled, attempts to insert invalid ENUM values result in an error.

• If an ENUM column is declared to permit NULL, the NULL value is a valid value for the column, and the default value is NULL. If an ENUM column is declared NOT NULL, its default value is the first element of the list of permitted values.

**Enumeration Sorting**

ENUM values are sorted based on their index numbers, which depend on the order in which the enumeration members were listed in the column specification. For example, 'b' sorts before 'a' for ENUM('b', 'a'). The empty string sorts before nonempty strings, and NULL values sort before all other enumeration values.

To prevent unexpected results when using the ORDER BY clause on an ENUM column, use one of these
techniques:

• Specify the ENUM list in alphabetic order.

• Make sure that the column is sorted lexically rather than by index number by coding ORDER BY CAST(col AS CHAR) or ORDER BY CONCAT(col).

**Enumeration Limitations**

An enumeration value cannot be an expression, even one that evaluates to a string value.

For example, this CREATE TABLE statement does not work because the CONCAT function cannot be used to construct an enumeration value:

```sql
CREATE TABLE sizes (
	size ENUM('small', CONCAT('med','ium'), 'large')
);
```

You also cannot employ a user variable as an enumeration value. This pair of statements do not work:

```sql
SET @mysize = 'medium';
CREATE TABLE sizes (
	size ENUM('small', @mysize, 'large')
);
```

We strongly recommend that you do not use numbers as enumeration values, because it does not save on storage over the appropriate TINYINT or SMALLINT type, and it is easy to mix up the strings and the underlying number values (which might not be the same) if you quote the ENUM values incorrectly. If you do use a number as an enumeration value, always enclose it in quotation marks. If the quotation marks are omitted, the number is regarded as an index. Duplicate values in the definition cause a warning, or an error if strict SQL mode is enabled.

### The SET Type

A SET is a string object that can have zero or more values, each of which must be chosen from a list of permitted values specified when the table is created. SET column values that consist of multiple set members are specified with members separated by commas (,). A consequence of this is that SET member values should not themselves contain commas.

For example, a column specified as SET('one', 'two') NOT NULL can have any of these values:

```sql
''
'one'
'two'
'one,two'
```

A SET column can have a maximum of 64 distinct members. A table can have no more than 255 unique element list definitions among its ENUM and SET columns considered as a group.

Duplicate values in the definition cause a warning, or an error if strict SQL mode is enabled.

Trailing spaces are automatically deleted from SET member values in the table definition when a table is created.

When retrieved, values stored in a SET column are displayed using the lettercase that was used in the column definition. Note that SET columns can be assigned a character set and collation. For binary or
case-sensitive collations, lettercase is taken into account when assigning values to the column.

MySQL stores SET values numerically, with the low-order bit of the stored value corresponding to the first set member. If you retrieve a SET value in a numeric context, the value retrieved has bits set corresponding to the set members that make up the column value. For example, you can retrieve numeric values from a SET column like this:

```sql
mysql> SELECT set_col+0 FROM tbl_name;
```

If a number is stored into a SET column, the bits that are set in the binary representation of the number determine the set members in the column value. For a column specified as SET('a','b','c','d'), the members have the following decimal and binary values.

| SET Member | Decimal Value | Binary Value |
| ---------- | ------------- | ------------ |
| 'a'        | 1             | 0001         |
| 'b'        | 2             | 0010         |
| 'c'        | 4             | 0100         |
| 'd'        | 8             | 1000         |

If you assign a value of 9 to this column, that is 1001 in binary, so the first and fourth SET value members 'a' and 'd' are selected and the resulting value is 'a,d'.

For a value containing more than one SET element, it does not matter what order the elements are listed in when you insert the value. It also does not matter how many times a given element is listed in the value. When the value is retrieved later, each element in the value appears once, with elements listed according to the order in which they were specified at table creation time. For example, suppose that a column is specified as SET('a','b','c','d'):

```sql
mysql> CREATE TABLE myset (col SET('a', 'b', 'c', 'd'));
```

If you insert the values 'a,d', 'd,a', 'a,d,d', 'a,d,a', and 'd,a,d':

```sql
mysql> INSERT INTO myset (col) VALUES
	-> ('a,d'), ('d,a'), ('a,d,a'), ('a,d,d'), ('d,a,d');
Query OK, 5 rows affected (0.01 sec)
Records: 5 Duplicates: 0 Warnings: 0
```

Then all these values appear as 'a,d' when retrieved:

```sql
mysql> SELECT col FROM myset;
+------+
|  col |
+------+
|  a,d |
|  a,d |
|  a,d |
|  a,d |
|  a,d |
+------+
5 rows in set (0.04 sec)
```

If you set a SET column to an unsupported value, the value is ignored and a warning is issued:

```sql
mysql> INSERT INTO myset (col) VALUES ('a,d,d,s');
Query OK, 1 row affected, 1 warning (0.03 sec)

mysql> SHOW WARNINGS;
+---------+------+------------------------------------------+
|   Level | Code |								    Message |
+---------+------+------------------------------------------+
| Warning | 1265 | Data truncated for column 'col' at row 1 |
+---------+------+------------------------------------------+
1 row in set (0.04 sec)
mysql> SELECT col FROM myset;
+------+
|  col |
+------+
|  a,d |
|  a,d |
|  a,d |
|  a,d |
|  a,d |
|  a,d |
+------+
6 rows in set (0.01 sec)
```

If strict SQL mode is enabled, attempts to insert invalid SET values result in an error. SET values are sorted numerically. NULL values sort before non-NULL SET values.

Functions such as SUM() or AVG() that expect a numeric argument cast the argument to a number if necessary. For SET values, the cast operation causes the numeric value to be used.

Normally, you search for SET values using the FIND_IN_SET() function or the LIKE operator:

```sql
mysql> SELECT * FROM tbl_name WHERE FIND_IN_SET('value',set_col)>0;
mysql> SELECT * FROM tbl_name WHERE set_col LIKE '%value%';
```

The first statement finds rows where set_col contains the value set member. The second is similar, but not the same: It finds rows where set_col contains value anywhere, even as a substring of another set member.
The following statements also are permitted:

```sql
mysql> SELECT * FROM tbl_name WHERE set_col & 1;
mysql> SELECT * FROM tbl_name WHERE set_col = 'val1,val2';
```

The first of these statements looks for values containing the first set member. The second looks for an exact match. Be careful with comparisons of the second type. Comparing set values to 'val1,val2' returns different results than comparing values to 'val2,val1'. You should specify the values in the same order they are listed in the column definition.

To determine all possible values for a SET column, use SHOW COLUMNS FROM tbl_name LIKE set_col and parse the SET definition in the Type column of the output.

## Spatial Data Types

## The JSON Data Type

## Data Type Default Values

The DEFAULT value clause in a data type specification indicates a default value for a column. With one exception, the default value must be a constant; it cannot be a function or an expression. This means, for example, that you cannot set the default for a date column to be the value of a function such as NOW() or CURRENT_DATE. The exception is that you can specify CURRENT_TIMESTAMP as the default for TIMESTAMP and DATETIME columns.

BLOB, TEXT, GEOMETRY, and JSON columns cannot be assigned a default value.

If a column definition includes no explicit DEFAULT value, MySQL determines the default value as follows:

If the column can take NULL as a value, the column is defined with an explicit DEFAULT NULL clause.

If the column cannot take NULL as the value, MySQL defines the column with no explicit DEFAULT clause. Exception: If the column is defined as part of a PRIMARY KEY but not explicitly as NOT NULL, MySQL creates it as a NOT NULL column (because PRIMARY KEY columns must be NOT NULL). Before MySQL 5.7.3, the column is also assigned a DEFAULT clause using the implicit default value. To prevent this, include an explicit NOT NULL in the definition of any PRIMARY KEY column.

For data entry into a NOT NULL column that has no explicit DEFAULT clause, if an INSERT or REPLACE statement includes no value for the column, or an UPDATE statement sets the column to NULL, MySQL handles the column according to the SQL mode in effect at the time:

• If strict SQL mode is enabled, an error occurs for transactional tables and the statement is rolled back. For nontransactional tables, an error occurs, but if this happens for the second or subsequent row of a multiple-row statement, the preceding rows will have been inserted.

• If strict mode is not enabled, MySQL sets the column to the implicit default value for the column data type.

Suppose that a table t is defined as follows:

```sql
CREATE TABLE t (i INT NOT NULL);
```

In this case, i has no explicit default, so in strict mode each of the following statements produce an error and no row is inserted. When not using strict mode, only the third statement produces an error; the implicit default is inserted for the first two statements, but the third fails because DEFAULT(i) cannot produce a value:

```sql
INSERT INTO t VALUES();
INSERT INTO t VALUES(DEFAULT);
INSERT INTO t VALUES(DEFAULT(i));
```

For a given table, you can use the SHOW CREATE TABLE statement to see which columns have an explicit DEFAULT clause.

Implicit defaults are defined as follows:

• For numeric types, the default is 0, with the exception that for integer or floating-point types declared with the AUTO_INCREMENT attribute, the default is the next value in the sequence.

• For date and time types other than TIMESTAMP, the default is the appropriate “zero” value for the type. This is also true for TIMESTAMP if the explicit_defaults_for_timestamp system variable is enabled. Otherwise, for the first TIMESTAMP column in a table, the default value is the current date and time.

• For string types other than ENUM, the default value is the empty string. For ENUM, the default is the first enumeration value.

## Data Type Storage Requirements

The storage requirements for table data on disk depend on several factors. Different storage engines represent data types and store raw data differently. Table data might be compressed, either for a column or an entire row, complicating the calculation of storage requirements for a table or column.

Despite differences in storage layout on disk, the internal MySQL APIs that communicate and exchange information about table rows use a consistent data structure that applies across all storage engines.

**Numeric Type Storage Requirements**

| Data Type                          | Storage Required                                  |
| ---------------------------------- | ------------------------------------------------- |
| TINYINT                            | 1 byte                                            |
| SMALLINT                           | 2 bytes                                           |
| MEDIUMINT                          | 3 bytes                                           |
| INT, INTEGER                       | 4 bytes                                           |
| BIGINT                             | 8 bytes                                           |
| FLOAT(p)                           | 4 bytes if 0 <= p <= 24, 8 bytes if 25 <= p <= 53 |
| FLOAT                              | 4 bytes                                           |
| DOUBLE [PRECISION], REAL           | 8 bytes                                           |
| DECIMAL(M,D), NUMERIC(M,D) Varies; | see following discussion                          |
| BIT(M)                             | approximately (M+7)/8 bytes                       |

Values for DECIMAL (and NUMERIC) columns are represented using a binary format that packs nine decimal (base 10) digits into four bytes. Storage for the integer and fractional parts of each value are determined separately. Each multiple of nine digits requires four bytes, and the “leftover” digits require some fraction of four bytes. The storage required for excess digits is given by the following table. 

| Leftover Digits | Number of Bytes |
| --------------- | --------------- |
| 0               | 0               |
| 1               | 1               |
| 2               | 1               |
| 3               | 2               |
| 4               | 2               |
| 5               | 3               |
| 6               | 3               |
| 7               | 4               |
| 8               | 4               |

**Date and Time Type Storage Requirements**

For TIME, DATETIME, and TIMESTAMP columns, the storage required for tables created before MySQL 5.6.4 differs from tables created from 5.6.4 on. This is due to a change in 5.6.4 that permits these types to have a fractional part, which requires from 0 to 3 bytes.

| Data Type | Storage Required Before MySQL 5.6.4 | Storage Required as of MySQL 5.6.4   |
| --------- | ----------------------------------- | ------------------------------------ |
| YEAR      | 1 byte                              | 1 byte                               |
| DATE      | 3 bytes                             | 3 bytes                              |
| TIME      | 3 bytes                             | 3 bytes + fractional seconds storage |
| DATETIME  | 8 bytes                             | 5 bytes + fractional seconds storage |
| TIMESTAMP | 4 bytes                             | 4 bytes + fractional seconds storage |

As of MySQL 5.6.4, storage for YEAR and DATE remains unchanged. However, TIME, DATETIME, and TIMESTAMP are represented differently. DATETIME is packed more efficiently, requiring 5 rather than 8 bytes for the nonfractional part, and all three parts have a fractional part that requires from 0 to 3 bytes,
depending on the fractional seconds precision of stored values.

| Fractional Seconds Precision | Storage Required |
| ---------------------------- | ---------------- |
| 0                            | 0 byte           |
| 1,2                          | 1 bytes          |
| 3,4                          | 2 bytes          |
| 5,6                          | 3 bytes          |

For example, TIME(0), TIME(2), TIME(4), and TIME(6) use 3, 4, 5, and 6 bytes, respectively. TIME and TIME(0) are equivalent and require the same storage.

**String Type Storage Requirements**

In the following table, M represents the declared column length in characters for nonbinary string types and bytes for binary string types. L represents the actual length in bytes of a given string value.

| Data Type                   | Storage Required                                             |
| --------------------------- | ------------------------------------------------------------ |
| CHAR(M)                     | M × w bytes, 0 <= M <= 255, where w is the number of bytes required for the maximum-length character in the character set. |
| BINARY(M)                   | M bytes, 0 <= M <= 255                                       |
| VARCHAR(M), VARBINARY(M)    | L + 1 bytes if column values require 0 − 255 bytes, L + 2 bytes if values may require more than 255 bytes |
| TINYBLOB, TINYTEXT          | L + 1 bytes, where L < 2<sup>8</sup>                         |
| BLOB, TEXT                  | L + 2 bytes, where L < 2<sup>16</sup>                        |
| MEDIUMBLOB, MEDIUMTEXT      | L + 3 bytes, where L < 2<sup>24</sup>                        |
| LONGBLOB, LONGTEXT          | L + 4 bytes, where L < 2<sup>32</sup>                        |
| ENUM('value1','value2',...) | 1 or 2 bytes, depending on the number of enumeration values (65,535 values maximum) |
| SET('value1','value2',...)  | 1, 2, 3, 4, or 8 bytes, depending on the number of set members (64 members maximum) |

Variable-length string types are stored using a length prefix plus data. The length prefix requires from one to four bytes depending on the data type, and the value of the prefix is L (the byte length of the string). For example, storage for a MEDIUMTEXT value requires L bytes to store the value plus three bytes to store the length of the value.

To calculate the number of bytes used to store a particular CHAR, VARCHAR, or TEXT column value, you must take into account the character set used for that column and whether the value contains multibyte characters. In particular, when using a utf8 Unicode character set, you must keep in mind that not all characters use the same number of bytes. utf8mb3 and utf8mb4 character sets can require up to three and four bytes per character, respectively.

VARCHAR, VARBINARY, and the BLOB and TEXT types are variable-length types. For each, the storage requirements depend on these factors:

• The actual length of the column value

• The column's maximum possible length

• The character set used for the column, because some character sets contain multibyte characters

For example, a VARCHAR(255) column can hold a string with a maximum length of 255 characters. Assuming that the column uses the latin1 character set (one byte per character), the actual storage required is the length of the string (L), plus one byte to record the length of the string. For the string 'abcd', L is 4 and the storage requirement is five bytes. If the same column is instead declared to use the ucs2 double-byte character set, the storage requirement is 10 bytes: The length of 'abcd' is eight bytes and the column requires two bytes to store lengths because the maximum length is greater than 255 (up to 510 bytes).

The effective maximum number of bytes that can be stored in a VARCHAR or VARBINARY column is subject to the maximum row size of 65,535 bytes, which is shared among all columns. For a VARCHAR column that stores multibyte characters, the effective maximum number of characters is less. For example, utf8mb3 characters can require up to three bytes per character, so a VARCHAR column that uses the utf8mb3 character set can be declared to be a maximum of 21,844 characters.

InnoDB encodes fixed-length fields greater than or equal to 768 bytes in length as variable-length fields, which can be stored off-page. For example, a CHAR(255) column can exceed 768 bytes if the maximum byte length of the character set is greater than 3, as it is with utf8mb4.

The size of an ENUM object is determined by the number of different enumeration values. One byte is used for enumerations with up to 255 possible values. Two bytes are used for enumerations having between 256 and 65,535 possible values.

The size of a SET object is determined by the number of different set members. If the set size is N, the object occupies (N+7)/8 bytes, rounded up to 1, 2, 3, 4, or 8 bytes. A SET can have a maximum of 64 members.

## Choosing the Right Type for a Column

For optimum storage, you should try to use the most precise type in all cases. For example, if an integer column is used for values in the range from 1 to 99999, MEDIUMINT UNSIGNED is the best type. Of the types that represent all the required values, this type uses the least amount of storage.

All basic calculations (+, -, *, and /) with DECIMAL columns are done with precision of 65 decimal (base 10) digits.

If accuracy is not too important or if speed is the highest priority, the DOUBLE type may be good enough. For high precision, you can always convert to a fixed-point type stored in a BIGINT. This enables you to do all calculations with 64-bit integers and then convert results back to floating-point values as necessary.

## Using Data Types from Other Database Engines