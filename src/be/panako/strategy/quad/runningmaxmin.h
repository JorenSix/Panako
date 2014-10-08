/**
 * Copyright 2006, Daniel Lemire
 * 
 */
/**
 *  This program is free software; you can
 *  redistribute it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation (version 2). This
 *  program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details. You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

#ifndef RUNNINGMAXMIN
#define RUNNINGMAXMIN
#include <cmath>
#include <cassert>
#include <vector>
#include <deque>
#include <map>
#include <algorithm>
#include <iostream>
#include <sstream>
#include <stdlib.h>
#include <fstream>
#include <string.h>

typedef unsigned int uint;

typedef double floattype;

using namespace std;

uint max(uint a, uint b) {
    if (a > b)
        return a;
    return b;
}
uint min(uint a, uint b) {
    if (a < b)
        return a;
    return b;
}

void display(deque<int> & a) {
    for (uint i = 0; i < a.size(); ++i)
        cout << a[i] << " ";
    cout << endl;
}

void display(vector<floattype> & a) {
    for (uint i = 0; i < a.size(); ++i)
        cout << a[i] << " ";
    cout << endl;
}

class minmaxfilter {
public:

    virtual vector<floattype>& getmaxvalues()=0;
    virtual vector<floattype>& getminvalues()=0;

    virtual ~minmaxfilter() {
    }
    ;
};

/**
 * This is the naive algorithm one might try first.
 */
class slowmaxmin: public minmaxfilter {
public:
    slowmaxmin(vector<floattype> & array, int width) :
        maxvalues(array.size() - width + 1),
                minvalues(array.size() - width + 1) {
        floattype maxvalue, minvalue;
        for (uint s = 0; s < array.size() - width + 1; ++s) {
            maxvalue = array[s];
            minvalue = array[s];
            // could be done with iterators
            for (uint k = s + 1; k < s + width; ++k) {
                if (maxvalue < array[k])
                    maxvalue = array[k];
                if (minvalue > array[k])
                    minvalue = array[k];
            }
            maxvalues[s] = maxvalue;
            minvalues[s] = minvalue;
        }
    }
    vector<floattype> & getmaxvalues() {
        return maxvalues;
    }
    vector<floattype> & getminvalues() {
        return minvalues;
    }
    vector<floattype> maxvalues;
    vector<floattype> minvalues;
};

/**
 * This is an implementation of the patented Gil-Kimmel algorithm.
 * Could be rewritten to use less memory.
 */
class GilKimmel: public minmaxfilter {
public:
    GilKimmel(vector<floattype> & array, int width) :
        maxvalues(array.size() - width + 1),
                minvalues(array.size() - width + 1) {
        vector<floattype> R(array.size(), -1);
        vector<floattype> S(array.size(), -1);
        computePrefixSuffixMax(R, S, array, width);// implements the cut in the middle trick
        for (int j = 0; j < (int) array.size() - width + 1; j += width) {
            const int endofblock = min(j + width,
                    (int) array.size() - width + 1);
            int begin = j;
            int end = endofblock;
            int midpoint = (end - begin + 1) / 2 + begin;
            while (midpoint != end) {
                if (S[midpoint + width - 1] <= R[midpoint]) {
                    begin = midpoint;
                    midpoint = (end - begin + 1) / 2 + begin;
                } else {
                    end = midpoint;
                    midpoint = (end - begin + 1) / 2 + begin;
                }
            }
            for (int jj = j; jj < midpoint; ++jj) {
                maxvalues[jj] = R[jj];
            }
            for (int jj = midpoint; jj < endofblock; ++jj) {
                maxvalues[jj] = S[jj + width - 1];
            }
        }
        computePrefixSuffixMin(R, S, array, width);// implements the cut in the middle trick
        for (int j = 0; j < (int) array.size() - width + 1; j += width) {
            const int endofblock = min(j + width,
                    (int) array.size() - width + 1);
            int begin = j;
            int end = endofblock;
            int midpoint = (end - begin + 1) / 2 + begin;
            while (midpoint != end) {
                if (S[midpoint + width - 1] >= R[midpoint]) {
                    begin = midpoint;
                    midpoint = (end - begin + 1) / 2 + begin;
                } else {
                    end = midpoint;
                    midpoint = (end - begin + 1) / 2 + begin;
                }
            }
            for (int jj = j; jj < midpoint; ++jj) {
                minvalues[jj] = R[jj];
            }
            for (int jj = midpoint; jj < endofblock; ++jj) {
                minvalues[jj] = S[jj + width - 1];
            }
        }
    }
    void computePrefixSuffixMax(vector<floattype>& R, vector<floattype>& S,
            const vector<floattype> & array, const int width) {
        for (int j = 0; j < (int) array.size(); j += width) {
            const int begin = j;
            const int end = min((int) array.size(), j + width);
            const int midpoint = (end - begin + 1) / 2 + begin;
            S[begin] = array[begin];
            for (int jj = begin + 1; jj < midpoint; ++jj) {
                S[jj] = max(array[jj], S[jj - 1]);
            }
            R[end - 1] = array[end - 1];
            for (int jj = end - 2; jj >= midpoint; --jj) {
                R[jj] = max(R[jj + 1], array[jj]);
            }
            if (max(R[midpoint], S[midpoint - 1]) == R[midpoint]) {
                for (int jj = midpoint; jj < end; ++jj)
                    S[jj] = max(array[jj], S[jj - 1]);
                for (int jj = midpoint - 1; jj >= begin; --jj)
                    R[jj] = R[midpoint];
            } else {
                for (int jj = midpoint - 1; jj >= begin; --jj)
                    R[jj] = max(R[jj + 1], array[jj]);
                for (int jj = midpoint; jj < end; ++jj)
                    S[jj] = S[midpoint - 1];
            }
        }
    }

    void computePrefixSuffixMin(vector<floattype>& R, vector<floattype>& S,
            const vector<floattype> & array, const int width) {
        for (int j = 0; j < (int) array.size(); j += width) {
            const int begin = j;
            const int end = min((int) array.size(), j + width);
            const int midpoint = (end - begin + 1) / 2 + begin;
            S[begin] = array[begin];
            for (int jj = begin + 1; jj < midpoint; ++jj) {
                S[jj] = min(array[jj], S[jj - 1]);
            }
            R[end - 1] = array[end - 1];
            for (int jj = end - 2; jj >= midpoint; --jj) {
                R[jj] = min(R[jj + 1], array[jj]);
            }
            if (min(R[midpoint], S[midpoint - 1]) == R[midpoint]) {
                for (int jj = midpoint; jj < end; ++jj)
                    S[jj] = min(array[jj], S[jj - 1]);
                for (int jj = midpoint - 1; jj >= begin; --jj)
                    R[jj] = R[midpoint];
            } else {
                for (int jj = midpoint - 1; jj >= begin; --jj)
                    R[jj] = min(R[jj + 1], array[jj]);
                for (int jj = midpoint; jj < end; ++jj)
                    S[jj] = S[midpoint - 1];
            }
        }
    }
    vector<floattype> & getmaxvalues() {
        return maxvalues;
    }
    vector<floattype> & getminvalues() {
        return minvalues;
    }
    vector<floattype> maxvalues;
    vector<floattype> minvalues;
};

/**
 * This should be very close to the van Herk algorithm.
 */
class vanHerkGilWermanmaxmin: public minmaxfilter {
public:
    vanHerkGilWermanmaxmin(vector<floattype> & array, int width) :
        maxvalues(array.size() - width + 1),
                minvalues(array.size() - width + 1) {
        vector<floattype> R(width);
        vector<floattype> S(width);
        for (uint j = 0; j < array.size() - width + 1; j += width) {
            uint Rpos = min(j + width - 1, array.size() - 1);
            R[0] = array[Rpos];
            for (uint i = Rpos - 1; i + 1 > j; i -= 1)
                R[Rpos - i] = max(R[Rpos - i - 1], array[i]);
            S[0] = array[Rpos];
            for (uint i = Rpos + 1; i < min(j + 2 * width - 1, array.size()); ++i) {
                S[i - Rpos] = max(S[i - Rpos - 1], array[i]);
            }
            for (uint i = 0; i < min(j + 2 * width - 1, array.size()) - Rpos; i
                    += 1)
                maxvalues[j + i] = max(S[i], R[(Rpos - j + 1) - i - 1]);
        }
        for (uint j = 0; j < array.size() - width + 1; j += width) {
            uint Rpos = min(j + width - 1, array.size() - 1);
            R[0] = array[Rpos];
            for (uint i = Rpos - 1; i + 1 > j; i -= 1)
                R[Rpos - i] = min(R[Rpos - i - 1], array[i]);
            S[0] = array[Rpos];
            for (uint i = Rpos + 1; i < min(j + 2 * width - 1, array.size()); ++i) {
                S[i - Rpos] = min(S[i - Rpos - 1], array[i]);
            }
            for (uint i = 0; i < min(j + 2 * width - 1, array.size()) - Rpos; i
                    += 1)
                minvalues[j + i] = min(S[i], R[(Rpos - j + 1) - i - 1]);
        }
        assert(maxvalues.size() == array.size() - width + 1);
        assert(minvalues.size() == array.size() - width + 1);
    }
    vector<floattype> & getmaxvalues() {
        return maxvalues;
    }
    vector<floattype> & getminvalues() {
        return minvalues;
    }
    vector<floattype> maxvalues;
    vector<floattype> minvalues;
};

/**
 * implementation of the streaming algorithm
 */
class lemiremaxmin: public minmaxfilter {
public:
    lemiremaxmin(vector<floattype> & array, uint width) :
        maxvalues(array.size() - width + 1),
                minvalues(array.size() - width + 1) {
        deque<int> maxfifo, minfifo;
        for (uint i = 1; i < width; ++i) {
            if (array[i] > array[i - 1]) { //overshoot
                minfifo.push_back(i - 1);
                while (!maxfifo.empty()) {
                    if (array[i] <= array[maxfifo.back()]) {
                        if (i == width + maxfifo.front())
                            maxfifo.pop_front();
                        break;
                    }
                    maxfifo.pop_back();
                }
            } else {
                maxfifo.push_back(i - 1);
                while (!minfifo.empty()) {
                    if (array[i] >= array[minfifo.back()]) {
                        if (i == width + minfifo.front())
                            minfifo.pop_front();
                        break;
                    }
                    minfifo.pop_back();
                }
            }
        }
        for (uint i = width; i < array.size(); ++i) {
            maxvalues[i - width]
                        = array[maxfifo.empty() ? i - 1 : maxfifo.front()];
            minvalues[i - width]
                        = array[minfifo.empty() ? i - 1 : minfifo.front()];
            if (array[i] > array[i - 1]) { //overshoot
                minfifo.push_back(i - 1);
                if (i == width + minfifo.front())
                    minfifo.pop_front();
                while (!maxfifo.empty()) {
                    if (array[i] <= array[maxfifo.back()]) {
                        if (i == width + maxfifo.front())
                            maxfifo.pop_front();
                        break;
                    }
                    maxfifo.pop_back();
                }
            } else {
                maxfifo.push_back(i - 1);
                if (i == width + maxfifo.front())
                    maxfifo.pop_front();
                while (!minfifo.empty()) {
                    if (array[i] >= array[minfifo.back()]) {
                        if (i == width + minfifo.front())
                            minfifo.pop_front();
                        break;
                    }
                    minfifo.pop_back();
                }
            }
        }
        maxvalues[array.size() - width]
                = array[maxfifo.empty() ? array.size() - 1 : maxfifo.front() ];
        minvalues[array.size() - width]
                = array[minfifo.empty() ? array.size() - 1 : minfifo.front() ];
    }
    vector<floattype> & getmaxvalues() {
        return maxvalues;
    }
    vector<floattype> & getminvalues() {
        return minvalues;
    }
    vector<floattype> maxvalues;
    vector<floattype> minvalues;
};




/**
 * implementation of the streaming algorithm
 */
class lemirebitmapmaxmin: public minmaxfilter {
public:
    //TODO: make the code portable to non-GCC-like compilers
    //TODO: extend beyond 64-bit to include 128-bit
    lemirebitmapmaxmin(vector<floattype> & array, const uint width) :
        maxvalues(array.size() - width + 1),
                minvalues(array.size() - width + 1) {
        assert(width <= sizeof(unsigned long)*8);
        unsigned long maxfifo = 0;
        const bool USEPOP = true;
        unsigned long minfifo = 0;
        for (uint i = 1; i < width; ++i) {
            minfifo <<= 1;
            maxfifo <<= 1;
            if (array[i] > array[i - 1]) { //overshoot
                minfifo |= 1;
                while (maxfifo != 0 ) {
                    if (USEPOP) {
                        const long t = maxfifo & -maxfifo;
                        const int bitpos = __builtin_popcountl(t - 1);
                        if (array[i] <= array[i - bitpos]) {
                            break;
                        }
                        maxfifo ^= t;
                    } else {
                        const int bitpos = __builtin_ctzl(maxfifo);
                        if (array[i] >= array[i - bitpos]) {
                            break;
                        }
                        maxfifo ^= (1l << bitpos);
                    }
                }
            } else {
                maxfifo |= 1;
                while (minfifo != 0 ) {
                    if (USEPOP) {
                        const long t = minfifo & -minfifo;
                        const int bitpos = __builtin_popcountl(t - 1);
                        if (array[i] >= array[i - bitpos]) {
                            break;
                        }
                        minfifo ^= t;
                    } else {
                        const int bitpos = __builtin_ctzl(minfifo);
                        if (array[i] >= array[i - bitpos]) {
                            break;
                        }
                        minfifo ^= (1l << bitpos);
                    }
                }
            }
        }
        unsigned long mask = ~0l;
        if(width < sizeof(unsigned long)*8) {
            mask = (1UL<<width) - 1;
        }
        for (uint i = width; i < array.size(); ++i) {
            maxfifo &= mask;
            minfifo &= mask;
            if(maxfifo == 0)
               maxvalues[i - width] = array[ i - 1 ];
            else {
                maxvalues[i - width] = array[ i - ( __builtin_clzl(maxfifo)-(sizeof(unsigned long)*8 - width)) ];
            }
            if(minfifo == 0)
                minvalues[i - width] = array[ i - 1 ];
            else {
                minvalues[i - width] = array[ i  - ( __builtin_clzl(minfifo)-(sizeof(unsigned long)*8 - width)) ];
            }
            minfifo <<= 1;
            maxfifo <<= 1;
            if (array[i] > array[i - 1]) { //overshoot
                minfifo |= 1;
                while (maxfifo != 0 ) {
                    if (USEPOP) {
                        const long t = maxfifo & -maxfifo;
                        const int bitpos = __builtin_popcountl(t - 1);
                        if (array[i] <= array[i - bitpos]) {
                            break;
                        }
                        maxfifo ^= t;
                    } else {
                        const int bitpos = __builtin_ctzl(maxfifo);
                        if (array[i] >= array[i - bitpos]) {
                            break;
                        }
                        maxfifo ^= (1l << bitpos);
                    }
                }
            } else {
                maxfifo |= 1;
                while (minfifo != 0 ) {
                    if (USEPOP) {
                        const long t = minfifo & -minfifo;
                        const int bitpos = __builtin_popcountl(t - 1);
                        if (array[i] >= array[i - bitpos]) {
                            break;
                        }
                        minfifo ^= t;
                    } else {
                        const int bitpos = __builtin_ctzl(minfifo);
                        if (array[i] >= array[i - bitpos]) {
                            break;
                        }
                        minfifo ^= (1l << bitpos);
                    }
                }
            }
        }
        if(maxfifo == 0)
            maxvalues[array.size() - width] = array[ array.size() - 1 ];
        else
            maxvalues[array.size() - width] = array[ array.size() - ( __builtin_clzl(maxfifo)-(sizeof(unsigned long)*8 - width)) ];
        if(minfifo == 0)
            minvalues[array.size() - width] = array[ array.size() - 1 ];
        else
            minvalues[array.size() - width] = array[ array.size()  - ( __builtin_clzl(minfifo)-(sizeof(unsigned long)*8 - width)) ];
    }
    vector<floattype> & getmaxvalues() {
        return maxvalues;
    }
    vector<floattype> & getminvalues() {
        return minvalues;
    }
    vector<floattype> maxvalues;
    vector<floattype> minvalues;
};


/**
 * simplest implementation (pseudocode-like)
 */
class simplelemiremaxmin: public minmaxfilter {
public:
    simplelemiremaxmin(vector<floattype> & array, uint width) :
        maxvalues(array.size() - width + 1),
                minvalues(array.size() - width + 1) {
        deque<int> maxfifo, minfifo;
        maxfifo.push_back(0);
        minfifo.push_back(0);
        for (uint i = 1; i < width; ++i) {
            if (array[i] > array[i - 1]) { //overshoot
                maxfifo.pop_back();
                while (!maxfifo.empty()) {
                    if (array[i] <= array[maxfifo.back()])
                        break;
                    maxfifo.pop_back();
                }
            } else {
                minfifo.pop_back();
                while (!minfifo.empty()) {
                    if (array[i] >= array[minfifo.back()])
                        break;
                    minfifo.pop_back();
                }
            }
            maxfifo.push_back(i);
            minfifo.push_back(i);
        }
        for (uint i = width; i < array.size(); ++i) {
            maxvalues[i - width] = array[maxfifo.front()];
            minvalues[i - width] = array[minfifo.front()];
            if (array[i] > array[i - 1]) { //overshoot
                maxfifo.pop_back();
                while (!maxfifo.empty()) {
                    if (array[i] <= array[maxfifo.back()])
                        break;
                    maxfifo.pop_back();
                }
            } else {
                minfifo.pop_back();
                while (!minfifo.empty()) {
                    if (array[i] >= array[minfifo.back()])
                        break;
                    minfifo.pop_back();
                }
            }
            maxfifo.push_back(i);
            minfifo.push_back(i);
            if (i == width + maxfifo.front())
                maxfifo.pop_front();
            else if (i == width + minfifo.front())
                minfifo.pop_front();
        }
        maxvalues[array.size() - width] = array[maxfifo.front()];
        minvalues[array.size() - width] = array[minfifo.front()];
    }
    vector<floattype> & getmaxvalues() {
        return maxvalues;
    }
    vector<floattype> & getminvalues() {
        return minvalues;
    }
    vector<floattype> maxvalues;
    vector<floattype> minvalues;
};

#endif
