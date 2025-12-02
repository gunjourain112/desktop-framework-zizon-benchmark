package monitor

import "sync"

// RingBuffer is a fixed-size buffer for float64 values.
// It is safe for concurrent readers if there is only one writer,
// provided the writer updates atomically or we use a mutex.
// Since UI reads and Monitor writes, we should use a RWMutex to be safe.
type RingBuffer struct {
	data     []float64
	capacity int
	head     int // points to the next write position
	full     bool
	mu       sync.RWMutex
}

// NewRingBuffer creates a new ring buffer with the given capacity.
func NewRingBuffer(capacity int) *RingBuffer {
	return &RingBuffer{
		data:     make([]float64, capacity),
		capacity: capacity,
		head:     0,
		full:     false,
	}
}

// Add adds a new value to the buffer.
func (r *RingBuffer) Add(val float64) {
	r.mu.Lock()
	defer r.mu.Unlock()

	r.data[r.head] = val
	r.head++
	if r.head >= r.capacity {
		r.head = 0
		r.full = true
	}
}

// Do iterates over the buffer in chronological order (oldest to newest)
// and calls the callback function for each value.
// It uses a read lock.
func (r *RingBuffer) Do(f func(float64)) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	if !r.full {
		for i := 0; i < r.head; i++ {
			f(r.data[i])
		}
		return
	}

	// If full, start from head (oldest) to capacity-1
	for i := r.head; i < r.capacity; i++ {
		f(r.data[i])
	}
	// Then from 0 to head-1
	for i := 0; i < r.head; i++ {
		f(r.data[i])
	}
}

// Count returns the number of elements currently in the buffer.
func (r *RingBuffer) Count() int {
	r.mu.RLock()
	defer r.mu.RUnlock()
	if r.full {
		return r.capacity
	}
	return r.head
}
