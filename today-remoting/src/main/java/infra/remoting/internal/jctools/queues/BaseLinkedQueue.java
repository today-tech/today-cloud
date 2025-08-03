/*
 * Copyright 2021 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package infra.remoting.internal.jctools.queues;

import java.util.AbstractQueue;
import java.util.Iterator;

import static infra.remoting.internal.jctools.queues.UnsafeAccess.UNSAFE;
import static infra.remoting.internal.jctools.queues.UnsafeAccess.fieldOffset;

abstract class BaseLinkedQueuePad0<E> extends AbstractQueue<E> implements MessagePassingQueue<E> {
  byte b000, b001, b002, b003, b004, b005, b006, b007; //  8b
  byte b010, b011, b012, b013, b014, b015, b016, b017; // 16b
  byte b020, b021, b022, b023, b024, b025, b026, b027; // 24b
  byte b030, b031, b032, b033, b034, b035, b036, b037; // 32b
  byte b040, b041, b042, b043, b044, b045, b046, b047; // 40b
  byte b050, b051, b052, b053, b054, b055, b056, b057; // 48b
  byte b060, b061, b062, b063, b064, b065, b066, b067; // 56b
  byte b070, b071, b072, b073, b074, b075, b076, b077; // 64b
  byte b100, b101, b102, b103, b104, b105, b106, b107; // 72b
  byte b110, b111, b112, b113, b114, b115, b116, b117; // 80b
  byte b120, b121, b122, b123, b124, b125, b126, b127; // 88b
  byte b130, b131, b132, b133, b134, b135, b136, b137; // 96b
  byte b140, b141, b142, b143, b144, b145, b146, b147; // 104b
  byte b150, b151, b152, b153, b154, b155, b156, b157; // 112b
  byte b160, b161, b162, b163, b164, b165, b166, b167; // 120b
  // byte b170,b171,b172,b173,b174,b175,b176,b177;//128b
  //    * drop 8b as object header acts as padding and is >= 8b *
}

// $gen:ordered-fields
abstract class BaseLinkedQueueProducerNodeRef<E> extends BaseLinkedQueuePad0<E> {
  static final long P_NODE_OFFSET =
          fieldOffset(BaseLinkedQueueProducerNodeRef.class, "producerNode");

  private volatile LinkedQueueNode<E> producerNode;

  final void spProducerNode(LinkedQueueNode<E> newValue) {
    UNSAFE.putObject(this, P_NODE_OFFSET, newValue);
  }

  final void soProducerNode(LinkedQueueNode<E> newValue) {
    UNSAFE.putOrderedObject(this, P_NODE_OFFSET, newValue);
  }

  final LinkedQueueNode<E> lvProducerNode() {
    return producerNode;
  }

  final boolean casProducerNode(LinkedQueueNode<E> expect, LinkedQueueNode<E> newValue) {
    return UNSAFE.compareAndSwapObject(this, P_NODE_OFFSET, expect, newValue);
  }

  final LinkedQueueNode<E> lpProducerNode() {
    return producerNode;
  }
}

abstract class BaseLinkedQueuePad1<E> extends BaseLinkedQueueProducerNodeRef<E> {
  byte b000, b001, b002, b003, b004, b005, b006, b007; //  8b
  byte b010, b011, b012, b013, b014, b015, b016, b017; // 16b
  byte b020, b021, b022, b023, b024, b025, b026, b027; // 24b
  byte b030, b031, b032, b033, b034, b035, b036, b037; // 32b
  byte b040, b041, b042, b043, b044, b045, b046, b047; // 40b
  byte b050, b051, b052, b053, b054, b055, b056, b057; // 48b
  byte b060, b061, b062, b063, b064, b065, b066, b067; // 56b
  byte b070, b071, b072, b073, b074, b075, b076, b077; // 64b
  byte b100, b101, b102, b103, b104, b105, b106, b107; // 72b
  byte b110, b111, b112, b113, b114, b115, b116, b117; // 80b
  byte b120, b121, b122, b123, b124, b125, b126, b127; // 88b
  byte b130, b131, b132, b133, b134, b135, b136, b137; // 96b
  byte b140, b141, b142, b143, b144, b145, b146, b147; // 104b
  byte b150, b151, b152, b153, b154, b155, b156, b157; // 112b
  byte b160, b161, b162, b163, b164, b165, b166, b167; // 120b
  byte b170, b171, b172, b173, b174, b175, b176, b177; // 128b
}

// $gen:ordered-fields
abstract class BaseLinkedQueueConsumerNodeRef<E> extends BaseLinkedQueuePad1<E> {
  private static final long C_NODE_OFFSET =
          fieldOffset(BaseLinkedQueueConsumerNodeRef.class, "consumerNode");

  private LinkedQueueNode<E> consumerNode;

  final void spConsumerNode(LinkedQueueNode<E> newValue) {
    consumerNode = newValue;
  }

  @SuppressWarnings("unchecked")
  final LinkedQueueNode<E> lvConsumerNode() {
    return (LinkedQueueNode<E>) UNSAFE.getObjectVolatile(this, C_NODE_OFFSET);
  }

  final LinkedQueueNode<E> lpConsumerNode() {
    return consumerNode;
  }
}

abstract class BaseLinkedQueuePad2<E> extends BaseLinkedQueueConsumerNodeRef<E> {
  byte b000, b001, b002, b003, b004, b005, b006, b007; //  8b
  byte b010, b011, b012, b013, b014, b015, b016, b017; // 16b
  byte b020, b021, b022, b023, b024, b025, b026, b027; // 24b
  byte b030, b031, b032, b033, b034, b035, b036, b037; // 32b
  byte b040, b041, b042, b043, b044, b045, b046, b047; // 40b
  byte b050, b051, b052, b053, b054, b055, b056, b057; // 48b
  byte b060, b061, b062, b063, b064, b065, b066, b067; // 56b
  byte b070, b071, b072, b073, b074, b075, b076, b077; // 64b
  byte b100, b101, b102, b103, b104, b105, b106, b107; // 72b
  byte b110, b111, b112, b113, b114, b115, b116, b117; // 80b
  byte b120, b121, b122, b123, b124, b125, b126, b127; // 88b
  byte b130, b131, b132, b133, b134, b135, b136, b137; // 96b
  byte b140, b141, b142, b143, b144, b145, b146, b147; // 104b
  byte b150, b151, b152, b153, b154, b155, b156, b157; // 112b
  byte b160, b161, b162, b163, b164, b165, b166, b167; // 120b
  byte b170, b171, b172, b173, b174, b175, b176, b177; // 128b
}

/**
 * A base data structure for concurrent linked queues. For convenience also pulled in common single
 * consumer methods since at this time there's no plan to implement MC.
 */
abstract class BaseLinkedQueue<E> extends BaseLinkedQueuePad2<E> {

  @Override
  public final Iterator<E> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return this.getClass().getName();
  }

  protected final LinkedQueueNode<E> newNode() {
    return new LinkedQueueNode<E>();
  }

  protected final LinkedQueueNode<E> newNode(E e) {
    return new LinkedQueueNode<E>(e);
  }

  /**
   * {@inheritDoc} <br>
   *
   * <p>IMPLEMENTATION NOTES:<br>
   * This is an O(n) operation as we run through all the nodes and count them.<br>
   * The accuracy of the value returned by this method is subject to races with producer/consumer
   * threads. In particular when racing with the consumer thread this method may under estimate the
   * size.<br>
   *
   * @see java.util.Queue#size()
   */
  @Override
  public final int size() {
    // Read consumer first, this is important because if the producer is node is 'older' than the
    // consumer
    // the consumer may overtake it (consume past it) invalidating the 'snapshot' notion of size.
    LinkedQueueNode<E> chaserNode = lvConsumerNode();
    LinkedQueueNode<E> producerNode = lvProducerNode();
    int size = 0;
    // must chase the nodes all the way to the producer node, but there's no need to count beyond
    // expected head.
    while (chaserNode != producerNode
            && // don't go passed producer node
            chaserNode != null
            && // stop at last node
            size < Integer.MAX_VALUE) // stop at max int
    {
      LinkedQueueNode<E> next;
      next = chaserNode.lvNext();
      // check if this node has been consumed, if so return what we have
      if (next == chaserNode) {
        return size;
      }
      chaserNode = next;
      size++;
    }
    return size;
  }

  /**
   * {@inheritDoc} <br>
   *
   * <p>IMPLEMENTATION NOTES:<br>
   * Queue is empty when producerNode is the same as consumerNode. An alternative implementation
   * would be to observe the producerNode.value is null, which also means an empty queue because
   * only the consumerNode.value is allowed to be null.
   *
   * @see MessagePassingQueue#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    LinkedQueueNode<E> consumerNode = lvConsumerNode();
    LinkedQueueNode<E> producerNode = lvProducerNode();
    return consumerNode == producerNode;
  }

  protected E getSingleConsumerNodeValue(
          LinkedQueueNode<E> currConsumerNode, LinkedQueueNode<E> nextNode) {
    // we have to null out the value because we are going to hang on to the node
    final E nextValue = nextNode.getAndNullValue();

    // Fix up the next ref of currConsumerNode to prevent promoted nodes from keeping new ones
    // alive.
    // We use a reference to self instead of null because null is already a meaningful value (the
    // next of
    // producer node is null).
    currConsumerNode.soNext(currConsumerNode);
    spConsumerNode(nextNode);
    // currConsumerNode is now no longer referenced and can be collected
    return nextValue;
  }

  @Override
  public E relaxedPoll() {
    final LinkedQueueNode<E> currConsumerNode = lpConsumerNode();
    final LinkedQueueNode<E> nextNode = currConsumerNode.lvNext();
    if (nextNode != null) {
      return getSingleConsumerNodeValue(currConsumerNode, nextNode);
    }
    return null;
  }

  @Override
  public E relaxedPeek() {
    final LinkedQueueNode<E> nextNode = lpConsumerNode().lvNext();
    if (nextNode != null) {
      return nextNode.lpValue();
    }
    return null;
  }

  @Override
  public boolean relaxedOffer(E e) {
    return offer(e);
  }

  @Override
  public int drain(Consumer<E> c) {
    long result = 0; // use long to force safepoint into loop below
    int drained;
    do {
      drained = drain(c, 4096);
      result += drained;
    }
    while (drained == 4096 && result <= Integer.MAX_VALUE - 4096);
    return (int) result;
  }

  @Override
  public int drain(Consumer<E> c, int limit) {
    LinkedQueueNode<E> chaserNode = this.lpConsumerNode();
    for (int i = 0; i < limit; i++) {
      final LinkedQueueNode<E> nextNode = chaserNode.lvNext();

      if (nextNode == null) {
        return i;
      }
      // we have to null out the value because we are going to hang on to the node
      final E nextValue = getSingleConsumerNodeValue(chaserNode, nextNode);
      chaserNode = nextNode;
      c.accept(nextValue);
    }
    return limit;
  }

  @Override
  public void drain(Consumer<E> c, WaitStrategy wait, ExitCondition exit) {
    LinkedQueueNode<E> chaserNode = this.lpConsumerNode();
    int idleCounter = 0;
    while (exit.keepRunning()) {
      for (int i = 0; i < 4096; i++) {
        final LinkedQueueNode<E> nextNode = chaserNode.lvNext();
        if (nextNode == null) {
          idleCounter = wait.idle(idleCounter);
          continue;
        }

        idleCounter = 0;
        // we have to null out the value because we are going to hang on to the node
        final E nextValue = getSingleConsumerNodeValue(chaserNode, nextNode);
        chaserNode = nextNode;
        c.accept(nextValue);
      }
    }
  }

  @Override
  public int capacity() {
    return UNBOUNDED_CAPACITY;
  }
}
