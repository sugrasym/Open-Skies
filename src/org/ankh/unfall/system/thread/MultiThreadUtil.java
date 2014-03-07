/*    
This file is part of jME Planet Demo.

jME Planet Demo is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation.

jME Planet Demo is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU General Public License
along with jME Planet Demo.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ankh.unfall.system.thread;

import java.util.concurrent.Semaphore;

/**
 * Set of function to optimize long process on multicore architecture
 * @author Yacine Petitprez
 */
public final class MultiThreadUtil
{
    public final static int PROCESSORS_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * Provide a fast disable optimizations below
     * */
    public static boolean DISABLE_MULTICORE = false;

    private MultiThreadUtil()
    {
    }

    /**
     * A "for" structure multithreaded.
     * This is equivalent at:
     * <code>
     * 	for(int i = start; i < end; i++) {
     *    //Do the code present in ForRunnable.run(i)
     *  }
     * </code>
     * @param start
     * @param end
     * @param loop
     */
    public final static void multiFor(int start, int end, final ForRunnable loop)
    {
        if (PROCESSORS_COUNT <= 1 || DISABLE_MULTICORE)
        {

            for (int i = start; i < end; ++i)
            {
                loop.run(i);
            }

        } else
        {
            //On partitionne pour chaque thread:
            int count = (end - start) / PROCESSORS_COUNT;
            int lastcount = count + (end - start) % PROCESSORS_COUNT; //Le dernier thread s'occupe d'un peu plus d'iterations (celles restantes)

            final Semaphore s = new Semaphore(PROCESSORS_COUNT - 1);

            for (int i = 0; i < PROCESSORS_COUNT - 1; ++i)
            {
                //Chaque thread prend un jeton
                s.acquireUninterruptibly();

                final int tstart = i * count;
                final int tend = i * count + count;

                final ForRunnable thisLoop = loop.copy();

                Thread t = new Thread(new Runnable()
                {
                    public void run()
                    {
                        for (int i = tstart; i < tend; i++)
                        {
                            thisLoop.run(i);
                        }

                        s.release();
                    }
                });

                t.start();
                Thread.yield();
            }


            int tstart = (PROCESSORS_COUNT - 1) * count;
            int tend = (PROCESSORS_COUNT - 1) * count + lastcount;


            for (int i = tstart; i < tend; ++i)
            {
                loop.run(i);
            }

            //Nous attendons que l'ensemble des jetons soit presents
            s.acquireUninterruptibly(PROCESSORS_COUNT - 1);
        }
    }
}
