package org.tycho.scraping

object myUtils {

  implicit def map2Props(map:Map[String,String]):java.util.Properties = {
    (new java.util.Properties /: map) {case (props, (k,v)) => props.put(k,v); props}
  }


}