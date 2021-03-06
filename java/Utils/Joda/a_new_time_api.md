### [Introduction to Joda-Time](http://www.joda.org/joda-time/)
在Java 8发行之前，Java的Date/Time API一直都很不好用，从最初简陋的java.util.Date到后来号称完备的java.util.Calendar，
在使用上都很不友好。Joda-Time为Java SE 8版本之前的Java版本提供了对Date/Time API的替换版本，并且成为这些Java版本中的事实标准，
用来弥补Java在这方面的不足。在Java 8版本发行之后，用户可以迁移到新引入的java.time包下，
值得一提的是Joda-Time的作者也参与了这个包的设计和实现。   
在生产中，很多Java项目的运行环境还停留在JDK7甚至更老的版本，所以我们有必要学习Joda-Time的一些知识，
这篇文章也从使用的角度做一些笔记。
我们可以使用maven很简单的引入Joda的依赖：
```        
    <dependency>
        <groupId>joda-time</groupId>
        <artifactId>joda-time</artifactId>
        <version>2.9.2</version>
    </dependency>
```
#### 基本概念
##### Instants
The most frequently used concept in Joda-Time is that of the instant. An Instant is defined as a moment 
in the datetime continuum specified as a number of milliseconds from 1970-01-01T00:00Z. 
This definition of milliseconds is consistent with that of the JDK in Date or Calendar. 
Interoperating between the two APIs is thus simple.

Within Joda-Time an instant is represented by the ReadableInstant interface. 
The main implementation of this interface, and the class that the average API 
user needs to be most familiar with, is DateTime. 
DateTime is immutable - and once created the values do not change. 
Thus, this class can safely be passed around and used in multiple threads without synchronization.

The millisecond instant can be converted to any date time field using a Chronology. 
To assist with this, methods are provided on DateTime that act as getters for the most common date and time fields.

We discuss the chronology concept a litte further on in this overview.

A companion mutable class to DateTime is MutableDateTime. 
Objects of this class can be modified and are not thread-safe.

Other implementations of ReadableInstant include Instant and the deprecated DateMidnight.

###### Fields
The main API of DateTime has been kept small, limited to just get methods for each calendar field. 
So, for instance, the 'day-of-year' calendar field would be retrieved by calling the getDayOfYear() method. 

###### Properties
There is much more power available, however, through the use of what is termed a property. 
Each calendar field is associated with such a property. 
Thus, 'day-of-year', whose value is directly returned by the method getDayOfYear(), 
is also associated with the property returned by the dayOfYear() method. 
The property class associated with DateTime is DateTime.Property.

##### Intervals
An interval in Joda-Time represents an interval of time from one instant to another instant. 
Both instants are fully specified instants in the datetime continuum, complete with time zone.

Intervals are implemented as half-open(左闭右开区间), 
which is to say that the start instant is inclusive but the end instant is exclusive. 
The end is always greater than or equal to the start. 
Both end-points are restricted to having the same chronology and the same time zone.

Two implementations are provided, Interval and MutableInterval, both are specializations of ReadableInterval.

##### Durations
A duration in Joda-Time represents a duration of time measured in milliseconds. 
The duration is often obtained from an interval.

Durations are a very simple concept, and the implementation is also simple. 
They have no chronology or time zone, and consist solely of the millisecond duration.

Durations can be added to an instant, or to either end of an interval to change those objects. 
In datetime maths you could say:

      instant  +  duration  =  instant
Currently, there is only one implementation of the ReadableDuration interface: Duration.

##### Periods
A period in Joda-Time represents a period of time defined in terms of fields, for example, 
3 years 5 months 2 days and 7 hours. This differs from a duration in that it is inexact 
in terms of milliseconds. A period can only be resolved to an exact number of milliseconds 
by specifying the instant (including chronology and time zone) it is relative to.

For example, consider a period of 1 month. If you add this period to the 1st February (ISO) 
then you will get the 1st March. If you add the same period to the 1st March you will get the 1st April. 
But the duration added (in milliseconds) in these two cases is very different.

As a second example, consider adding 1 day at the daylight savings boundary. 
If you use a period to do the addition then either 23 or 25 hours will be added as appropriate. 
If you had created a duration equal to 24 hours, then you would end up with the wrong result.

Periods are implemented as a set of int fields. The standard set of fields in a period are 
years, months, weeks, days, hours, minutes, seconds and millis. 
The PeriodType class allows this set of fields to be restricted, for example to elimate weeks. 
This is significant when converting a duration or interval to a period, 
as the calculation needs to know which period fields it should populate.

Methods exist on periods to obtain each field value. 
Periods are not associated with either a chronology or a time zone.

Periods can be added to an instant, or to either end of an interval to change those objects. 
In datetime maths you could say:

      instant  +  period  =  instant
There are two implementations of the ReadablePeriod interface, Period and MutablePeriod.

##### Chronology
The Joda-Time design is based around the Chronology.
It is a calculation engine that supports the complex rules for a calendar system. 
It encapsulates the field objects, which are used on demand to split the absolute 
time instant into recognisable calendar fields like 'day-of-week'. 
It is effectively a pluggable calendar system.

The actual calculations of the chronology are split between the Chronology class itself 
and the field classes - DateTimeField and DurationField. 
Together, the subclasses of these three classes form the bulk of the code in the library. 
Most users will never need to use or refer directly to the subclasses. 
Instead, they will simply obtain the chronology and use it as a singleton, as follows:

    Chronology coptic = CopticChronology.getInstance();
Internally, all the chronology, field, etc. classes are maintained as singletons. 
Thus there is an initial setup cost when using Joda-Time, 
but after that only the main API instance classes (DateTime, Interval, Period, etc.) 
have creation and garbage collector costs.

Although the Chronology is key to the design, it is not key to using the API !!
For most applications, the Chronology can be ignored as it will default to the ISOChronology. 
This is suitable for most uses. You would change it if you need accurate dates before October 15, 1582, 
or whenever the Julian calendar ceased in the territory you're interested in. 
You'd also change it if you need a specific calendar like the Coptic calendar illustrated earlier.

##### TimeZones
The chronology class also supports the time zone functionality. 
This is applied to the underlying chronology via the decorator design pattern(装饰者模式). 
The DateTimeZone class provides access to the zones primarily through one factory method, as follows:

    DateTimeZone zone = DateTimeZone.forID("Europe/London");
In addition to named time zones, Joda-Time also supports fixed time zones. 
The simplest of these is UTC, which is defined as a constant:

    DateTimeZone zoneUTC = DateTimeZone.UTC;
Other fixed offset time zones can be obtained by a specialise factory method:
    
    DateTimeZone zoneUTC = DateTimeZone.forOffsetHours(hours);
The time zone implementation is based on data provided by the public 
IANA(国际互联网代理成员管理局) time zone database. 

Joda-Time provides a default time zone which is used in many operations when a time zone is not specified. 
This is similar in concept to the default time zone of the java.util.TimeZone class. 
The value can be accessed and updated via static methods:

    DateTimeZone defaultZone = DateTimeZone.getDefault();
    DateTimeZone.setDefault(myZone);
    
##### Interface usage
As you have seen, Joda-Time defines a number of new interfaces which are visible throughout the javadocs. 
The most important is ReadableInstant which currently has 4 implementations. 
Other significant interfaces include ReadableInterval and ReadablePeriod. 
These are currently used as generalizations for a value-only and a mutable class, respectively.

An important point to mention here is that the Joda interfaces are used differently than 
the JDK Collections Framework interfaces. When working with a Collections interface, 
such as List or Map you will normally hold your variable as a type of List or Map, 
only referencing the concrete class when you create the object.

    List list = new ArrayList();
    Map map = new HashMap();
In Joda-Time, the interfaces exist to allow interoperation between similar date implementations, 
such as a mutable and immutable version of a class. As such, 
they only offer a subset of the methods of the concrete class. 
For most work, you will reference the concrete class, not the interface. 
This gives access to the full power of the library.
    
    DateTime dt = new DateTime();
For maximum flexibility however, you might choose to declare your method parameters 
using the Joda-Time interface. A method on the interface can obtain the concrete class for use within the method.

    public void process(ReadableDateTime dateTime) {
        DateTime dt = dateTime.toDateTime();
    }

#### 使用
##### Construction
A datetime object is created by using a DateTime constructor. The default constructor is used as follows

    DateTime dt = new DateTime();
and creates a datetime object representing the current date and time in milliseconds as determined 
by the system clock. It is constructed using the ISO calendar in the default time zone.
To create a datetime object representing a specific date and time, you may use an initialization string:

    DateTime dt = new DateTime("2004-12-13T21:39:45.618-08:00");
The initialization string must be in a format that is compatible with the ISO8601 standard.
DateTime also provides other constructors to create a specific date and time using a variety of standard fields. 
This also permits the use of any calendar and timezone.

##### JDK Interoperability
**无缝转换Joda-Time和JDK Date**    
The DateTime class has a constructor which takes an Object as input. 
In particular this constructor can be passed a JDK Date, JDK Calendar 
or JDK GregorianCalendar (It also accepts an ISO8601 formatted String, or Long object representing milliseconds). 
This is one half of the interoperability with the JDK. 
The other half of interoperability with JDK is provided by DateTime methods which return JDK objects.

Thus inter-conversion between Joda DateTime and JDK Date can be performed as follows

    // from Joda to JDK
    DateTime dt = new DateTime();
    Date jdkDate = dt.toDate();

    // from JDK to Joda
    dt = new DateTime(jdkDate);
Similarly, for JDK Calendar:

    // from Joda to JDK
    DateTime dt = new DateTime();
    Calendar jdkCal = dt.toCalendar(Locale.CHINESE);

    // from JDK to Joda
    dt = new DateTime(jdkCal);
and JDK GregorianCalendar:

    // from Joda to JDK
    DateTime dt = new DateTime();
    GregorianCalendar jdkGCal = dt.toGregorianCalendar();

    // from JDK to Joda
    dt = new DateTime(jdkGCal);

##### Querying DateTimes
The separation of the calculation of calendar fields (DateTimeField)
 from the representation of the calendar instant (DateTime) makes for a powerful and flexible API. 
 The connection between the two is maintained by the property (DateTime.Property) which provides access to the field.

For instance, the direct way to get the day of week for a particular DateTime, involves calling the method

    int iDoW = dt.getDayOfWeek();
where iDoW can take the values (from class DateTimeConstants).

    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;
    public static final int SUNDAY = 7;

###### Accessing fields
The direct methods are fine for simple usage, but more flexibility can be achieved 
via the property/field mechanism. The day of week property is obtained by

    DateTime.Property pDoW = dt.dayOfWeek();
which can be used to get richer information about the field, such as

    String strST = pDoW.getAsShortText(); // returns "Mon", "Tue", etc.
    String strT = pDoW.getAsText(); // returns "Monday", "Tuesday", etc.
which return short and long name strings (based on the current locale) of the day-of-week. 
Localized versions of these methods are also available, thus

    String strTF = pDoW.getAsText(Locale.FRENCH); // returns "Lundi", etc.
can be used to return the day-of-week name string in French.
Of course, the original integer value of the field is still accessible as

    iDoW = pDoW.get();
The property also provides access to other values associated with the field 
such as metadata on the minimum and maximum text size, leap status, related durations, etc. 
For a complete reference, see the documentation for the base class AbstractReadableInstantFieldProperty.
In practice, one would not actually create the intermediate pDoW variable. 
The code is easier to read if the methods are called on anonymous intermediate objects. Thus, for example,

    strT = dt.dayOfWeek().getAsText();
    iDoW = dt.dayOfWeek().get();
would be written instead of the more indirect code presented earlier.
Note: For the single case of getting the numerical value of a field, 
we recommend using the get method on the main DateTime object as it is more efficient.

    iDoW = dt.getDayOfWeek();
    
###### Date fields
The DateTime implementation provides a complete list of standard calendar fields:

    dt.getEra();
    dt.getYear();
    dt.getWeekyear();
    dt.getCenturyOfEra();
    dt.getYearOfEra();
    dt.getYearOfCentury();
    dt.getMonthOfYear();
    dt.getWeekOfWeekyear();
    dt.getDayOfYear();
    dt.getDayOfMonth();
    dt.getDayOfWeek();
Each of these also has a corresponding property method, which returns a DateTime.Property 
binding to the appropriate field, such as year() or monthOfYear(). 
The fields represented by these properties behave pretty much as their names would suggest. 
As you would expect, all the methods we showed above in the day-of-week example can be 
applied to any of these properties. For example, to extract the standard month, 
day and year fields from a datetime, we can write

    String month = dt.monthOfYear().getAsText();
    int maxDay = dt.dayOfMonth().getMaximumValue();
    boolean leapYear = dt.yearOfEra().isLeap();
    
###### Time fields
Another set of properties access fields representing intra-day durations for time calculations. 
Thus to compute the hours, minutes and seconds of the instant represented by a DateTime, we would write

    int hour = dt.getHourOfDay();
    int min = dt.getMinuteOfHour();
    int sec = dt.getSecondOfMinute();
Again each of these has a corresponding property method for more complex manipulation. 
The complete list of time fields can be found in the field reference.

##### Manipulating DateTimes
DateTime objects have value semantics, and cannot be modified after construction (they are immutable). 
Therefore, most simple manipulation of a datetime object involves construction of a new datetime 
as a modified copy of the original.

###### Modifying fields
One way to do this is to use methods on properties. To return to our prior example, 
if we wish to modify the dt object by changing its day-of-week field to Monday 
we can do so by using the setCopy method of the property:

    DateTime result = dt.dayOfWeek().setCopy(DateTimeConstants.MONDAY);
Note: If the DateTime object is already set to Monday then the same object will be returned.
To add to a date you could use the addToCopy method.

    DateTime result = dt.dayOfWeek().addToCopy(3);
    
###### DateTime methods
Another means of accomplishing similar calculations is to use methods on the DateTime object itself. 
Thus we could add 3 days to dt directly as follows:

    DateTime result = dt.plusDays(3);
###### Using a MutableDateTime
The methods outlined above are suitable for simple calculations involving one or two fields. 
In situations where multiple fields need to be modified, 
it is more efficient to create a mutable copy of the datetime, 
modify the copy and finally create a new value datetime.

    MutableDateTime mdt = dt.toMutableDateTime();
    // perform various calculations on mdt
    ...
    DateTime result = mdt.toDateTime();
MutableDateTime has a number of methods, including standard setters, for directly modifying the datetime.

##### Changing TimeZone
DateTime comes with support for a couple of common timezone calculations. 
For instance, if you want to get the local time in London at this very moment, you would do the following

    // get current moment in default time zone
    DateTime dt = new DateTime();
    // translate to London local time
    DateTime dtLondon = dt.withZone(DateTimeZone.forID("Europe/London"));
where DateTimeZone.forID("Europe/London") returns the timezone value for London. 
The resulting value dtLondon has the same absolute millisecond time, but a different set of field values.
There is also support for the reverse operation, i.e. to get the datetime (absolute millisecond) 
corresponding to the moment when London has the same local time as exists in the default time zone now. 
This is done as follows

    // get current moment in default time zone
    DateTime dt = new DateTime();
    // find the moment when London will have / had the same time
    dtLondonSameTime = dt.withZoneRetainFields(DateTimeZone.forID("Europe/London"));
A set of all TimeZone ID strings (such as "Europe/London") may be obtained by calling DateTimeZone.getAvailableIDs(). 

##### Changing Chronology
The DateTime class also has one method for changing calendars. 
This allows you to change the calendar for a given moment in time. 
Thus if you want to get the datetime for the current time, but in the Buddhist Calendar, you would do

    // get current moment in default time zone
    DateTime dt = new DateTime();
    dt.getYear();  // returns 2004
    // change to Buddhist chronology
    DateTime dtBuddhist = dt.withChronology(BuddhistChronology.getInstance());
    dtBuddhist.getYear();  // returns 2547
where BuddhistChronology.getInstance is a factory method for obtaining a Buddhist chronology.

##### Input and Output
Reading date time information from external sources which have their own custom format 
is a frequent requirement for applications that have datetime computations. 
Writing to a custom format is also a common requirement.

Many custom formats can be represented by date-format strings 
which specify a sequence of calendar fields along with the 
representation (numeric, name string, etc) and the field length. 
For example the pattern "yyyy" would represent a 4 digit year. 
Other formats are not so easily represented. 
For example, the pattern "yy" for a two digit year does not uniquely identify the century it belongs to. 
On output, this will not cause problems, but there is a problem of interpretation on input.

In addition, there are several date/time serialization standards in common use today, 
in particular the ISO8601. These must also be supported by most datetime applications.

Joda-Time supports these different requirements through a flexible architecture. 
We will now describe the various elements of this architecture.

##### Formatters
All printing and parsing is performed using a DateTimeFormatter object. 
Given such an object fmt, parsing is performed as follows

    String strInputDateTime;
    // string is populated with a date time string in some fashion
    ...
    DateTime dt = fmt.parseDateTime(strInputDateTime);
Thus a DateTime object is returned from the parse method of the formatter. 
Similarly, output is performed as

    String strOutputDateTime = fmt.print(dt);
###### Standard Formatters
Support for standard formats based on ISO8601 is provided by the ISODateTimeFormat class. 
This provides a number of factory methods.

For example, if you wanted to use the ISO standard format for datetime, 
which is yyyy-MM-dd'T'HH:mm:ss.SSSZZ, you would initialize fmt as

    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
You would then use fmt as described above, to read or write datetime objects in this format.
###### Custom Formatters
If you need a custom formatter which can be described in terms of a format pattern, 
you can use the factory method provided by the DateTimeFormat class. 
Thus to get a formatter for a 4 digit year, 2 digit month and 2 digit day of month, 
i.e. a format of yyyyMMdd you would do

    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd");
The pattern string is compatible with JDK date patterns.
You may need to print or parse in a particular Locale. 
This is achieved by calling the withLocale method on a formatter, 
which returns another formatter based on the original.

    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd");
    DateTimeFormatter frenchFmt = fmt.withLocale(Locale.FRENCH);
    DateTimeFormatter germanFmt = fmt.withLocale(Locale.GERMAN);
Formatters are immutable, so the original is not altered by the withLocale method.
###### Freaky Formatters
Finally, if you have a format that is not easily represented by a pattern string, 
Joda-Time architecture exposes a builder class that can be used to build a custom 
formatter which is programatically defined. 
Thus if you wanted a formatter to print and parse dates of the form "22-Jan-65", 
you could do the following:

    DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .appendDayOfMonth(2)
            .appendLiteral('-')
            .appendMonthOfYearShortText()
            .appendLiteral('-')
            .appendTwoDigitYear(1956)  // pivot = 1956
            .toFormatter();
Each append method appends a new field to be parsed/printed to the calling builder and returns a new builder. 
The final toFormatter method creates the actual formatter that will be used to print/parse.
What is particularly interesting about this format is the two digit year. 
Since the interpretation of a two digit year is ambiguous, 
the appendTwoDigitYear takes an extra parameter that defines the 100 year range of the two digits, 
by specifying the mid point of the range. 
In this example the range will be (1956 - 50) = 1906, to (1956 + 49) = 2005. 
Thus 04 will be 2004 but 07 will be 1907. This kind of conversion is not possible with ordinary format strings, 
highlighting the power of the Joda-Time formatting architecture.

###### Direct access
To simplify the access to the formatter architecture, 
methods have been provided on the datetime classes such as DateTime.

    DateTime dt = new DateTime();
    String a = dt.toString();
    String b = dt.toString("dd:MM:yy");
    String c = dt.toString("EEE", Locale.FRENCH);
    DateTimeFormatter fmt = ...;
    String d = dt.toString(fmt);
Each of the four results demonstrates a different way to use the formatters. 
Result a is the standard ISO8601 string for the DateTime. 
Result b will output using the pattern 'dd:MM:yy' (note that patterns are cached internally). 
Result c will output using the pattern 'EEE' in French. 
Result d will output using the specified formatter, and is thus the same as fmt.print(dt).
