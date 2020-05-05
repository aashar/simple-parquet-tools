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

package com.advaim.datamgmt.simpleparquettools.cli.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Formats {
  public enum Format {
    PARQUET,
    AVRO,
    SEQUENCE,
    TEXT
  }

  public static Format detectFormat(InputStream stream) throws IOException {
    byte[] first3 = new byte[3];
    stream.read(first3);
    if (Arrays.equals(first3, new byte[]{'P', 'A', 'R'})) {
      return Format.PARQUET;
    } else if (Arrays.equals(first3, new byte[]{'O', 'b', 'j'})) {
      return Format.AVRO;
    } else if (Arrays.equals(first3, new byte[]{'S', 'E', 'Q'})) {
      return Format.SEQUENCE;
    } else {
      return Format.TEXT;
    }
  }
}
