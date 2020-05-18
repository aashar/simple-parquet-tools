/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.advaim.datamgmt.simpleparquettools.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.data.TimeConversions.DateConversion;
import org.apache.avro.data.TimeConversions.TimestampMicrosConversion;
import org.apache.avro.data.TimeConversions.TimestampMillisConversion;
import org.apache.avro.generic.GenericData;

import com.advaim.datamgmt.simpleparquettools.cli.BaseCommand;

import com.advaim.datamgmt.simpleparquettools.cli.util.Expressions;
import org.slf4j.Logger;
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

import static com.advaim.datamgmt.simpleparquettools.cli.util.Expressions.select;

@Parameters(commandDescription = "Print the first N records from a file")
public class CatCommand extends BaseCommand {

  @Parameter(description = "<file>")
  List<String> sourceFiles;

  @Parameter(names={"-n", "--num-records"},
      description="The number of records to print")
  long numRecords;

  @Parameter(
      names = {"-c", "--column", "--columns"},
      description = "List of columns")
  List<String> columns;

  @Parameter(
	      names={"-f", "--flattened"},
	      description="Flatten output, CSV format")
  boolean flattened = false;

  public CatCommand(Logger console, long defaultNumRecords) {
    super(console);
    this.numRecords = defaultNumRecords;
  }

  @Override
  public int run() throws IOException {
    Preconditions.checkArgument(
        sourceFiles != null && !sourceFiles.isEmpty(),
        "Missing file name");
    Preconditions.checkArgument(sourceFiles.size() == 1,
        "Only one file can be given");

    final String source = sourceFiles.get(0);

    Schema schema = getAvroSchema(source);
    Schema projection = Expressions.filterSchema(schema, columns);

    if(flattened) {
	  console.info(getHeader(projection));
    }

    Iterable<Object> reader = openDataFile(source, projection);
    boolean threw = true;
    long count = 0;
    try {
      for (Object record : reader) {
        if (numRecords > 0 && count >= numRecords) {
          break;
        }
	    if (columns == null || columns.size() != 1) {
	       if (flattened && record.getClass().equals(GenericData.Record.class)) {
	    	 console.info(getCSVRecords(projection, (GenericData.Record)record));
	       } else {
	    	 console.info(String.valueOf(record));
	       }
		} else {
    	   console.info(String.valueOf(select(projection, record, columns.get(0))));
      	}
        count += 1;
      }
      threw = false;
    } catch (RuntimeException e) {
      throw new RuntimeException("Failed on record " + count, e);
    } finally {
      if (reader instanceof Closeable) {
        Closeables.close((Closeable) reader, threw);
      }
    }

    return 0;
  }

  String getHeader(Schema projection) {
	StringBuilder buff = new StringBuilder();
	projection.getFields().forEach(f -> {
		buff.append(f.name()+",");
	});
	
	buff.deleteCharAt(buff.length()-1);

	return buff.toString();
  }

  String getCSVRecords(Schema projection, GenericData.Record genericRecord) {
 	StringBuilder buff = new StringBuilder();
 	for (Field f : projection.getFields()) {
 	  Object fieldVal = genericRecord.get(f.pos());
 	  if(f.schema().getProp("datetimeFormat") != null) {
 		buff.append(
			getDatetime(f.schema().getLogicalType().getName(), f.schema().getProp("datetimeFormat"), f.schema().getProp("datetimeLanguage"), (Long)fieldVal)
		);
 	  } else if(f.schema().getProp("dateFormat") != null) {
  		buff.append(
 			getDate(f.schema().getProp("datetimeFormat"), (Integer)fieldVal)
 		);
  	  } else {
 		buff.append(String.valueOf(fieldVal)+",");
 	  }
 	}
 	buff.deleteCharAt(buff.length()-1);
	return buff.toString();
  }

  String getDate(String dateFormatString, Integer date) {
	if(dateFormatString.equalsIgnoreCase("EPOCH_DAYS")) {
	  LocalDate d = LocalDate.ofEpochDay(date);
	  return d.toString();
	} else {
	  DateTimeFormatter format = DateTimeFormatter.ofPattern(dateFormatString);
	  LocalDate dt = new DateConversion().fromInt(date, null, null);
	  return format.format(dt);
	}
  }

  String getDatetime(String logicalType, String datetimeFormatString, String datetimeLanguage, Long datetime) {
    DateTimeFormatter format;

	DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern(datetimeFormatString);

	if(datetimeLanguage == null || datetimeLanguage.isEmpty())
		format = formatBuilder.toFormatter(Locale.getDefault());
	else {
		Locale locale = new Locale.Builder()
			.setLanguage(datetimeLanguage)
			.build();
		format = formatBuilder.toFormatter(locale);
	}

  	if (logicalType.equalsIgnoreCase("timestamp-micros")) {
  		Instant i = new TimestampMicrosConversion().fromLong(datetime, null, null);
  		return format.format(i);
	}
	else if (logicalType.equalsIgnoreCase("timestamp-millis")) {
		Instant i = new TimestampMillisConversion().fromLong(datetime, null, null);
  		return format.format(i);
	} else {
		return null;
	}
  }

  @Override
  public List<String> getExamples() {
    return Lists.newArrayList(
        "# Show the first 10 records in file \"data.avro\":",
        "data.avro",
        "# Show the first 50 records in file \"data.parquet\":",
        "data.parquet -n 50"
    );
  }
}

