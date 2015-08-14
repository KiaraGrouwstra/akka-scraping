package org.tycho.scraping

import lombok.AccessLevel.PRIVATE
import java.io.Serializable
import java.util.NoSuchElementException
import lombok.Getter
import lombok.NonNull
import lombok.RequiredArgsConstructor
import lombok.experimental.Accessors
import org.apache.commons.pool2.ObjectPool
//remove if not needed
import scala.collection.JavaConversions._
import java.util.concurrent.atomic.AtomicReference

@RequiredArgsConstructor
abstract class LazySerializableObjectPool[T] extends ObjectPool[T] with Serializable {

//  @NonNull
//  @Getter(lazy = true, value = PRIVATE)
//  @Accessors(fluent = true)
//  @transient private val delegate = createDelegate()

  private val delegate = new java.util.concurrent.atomic.AtomicReference[ObjectPool[T]]

  def getDelegate(): ObjectPool[T] = {
    var value = this.delegate.get
    if (value == null) {
//      synchronized(this.delegate) {
//        value = this.delegate.get
//        if (value == null) {
          val actualValue = createDelegate()
          value = if (actualValue == null) this.delegate.get else actualValue
          this.delegate.set(value)
//        }
//      }
    }
//    (if (value == this.delegate.get) null else value).asInstanceOf[ObjectPool[T]]
    value
//    value.asInstanceOf[ObjectPool[T]]
  }

  def setDelegate(delegate: ObjectPool[T]) {
//    this.delegate = delegate
    this.delegate.set(delegate)
  }
  
  
//  def createDelegate(): ObjectPool[T]
  protected def createDelegate(): ObjectPool[T]

//  override def borrowObject(): T = delegate.borrowObject()
  override def borrowObject(): T = delegate.get().borrowObject()

  override def returnObject(obj: T) {
    delegate.get().returnObject(obj)
  }

  override def invalidateObject(obj: T) {
    delegate.get().invalidateObject(obj)
  }

  override def addObject() {
    delegate.get().addObject()
  }

  override def getNumIdle(): Int = delegate.get().getNumIdle

  override def getNumActive(): Int = delegate.get().getNumActive

  override def clear() {
    delegate.get().clear()
  }

  override def close() {
    delegate.get().close()
  }
}
