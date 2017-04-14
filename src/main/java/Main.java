import com.google.common.collect.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.google.common.math.IntMath.factorial;
import static java.lang.Math.abs;

/**
 * Created by aakoch on 2017-04-09.
 *
 * @author aakoch
 */
public class Main {
    private static Set<Choices> groups = new HashSet<>();

    private static int permutations = 0;
    private static double totalPermutations;

    public static void main(String[] args) {

        List<Den> dens = Arrays.asList(
                Den.of(0, 9, "Lion"),
                Den.of(1, 9, "Bear"),
                Den.of(2, 7, "Tiger"),
                Den.of(3, 9, "Bear"),
                //                Den.of(4, 0),
                Den.of(5, 5, "Bear"),
                Den.of(6, 6, "Tiger"),
                Den.of(7, 12, "Wolf"),
                Den.of(8, 2, "Wolf"),
                Den.of(9, 11, "Tiger"),
                //                Den.of(10, 0),
                //                Den.of(11, 0),
                Den.of(12, 14, "Webelos"),
                Den.of(13, 17, "Wolf")
        );


        totalPermutations = factorial(dens.size());
        System.out.println("totalPermutations = " + totalPermutations);

        int delta = 1;
        do  {
            System.out.println("delta = " + delta);
            permutations = 0;
            permutation(new ArrayList<>(), dens, delta);
            delta *= 2;
        } while (groups.size() == 0);

        System.out.println("permutations = " + permutations);

        ensureDen8WithAnotherWolfPack(groups);
        System.out.println("choices = " + groups.size());

        List<Choices> groupsList = new ArrayList<>(groups);
        Collections.sort(groupsList);

        int i = 0;
        for (Choices choice : groupsList) {
            System.out.format("choice %d = %n%s%n", ++i, choice);
        }
    }

    private static void permutation(List<Den> den, List<Den> dens, int delta) {
        int n = dens.size();
        if (n == 0) {
            if ((permutations++ / totalPermutations) * 1000 % 10 == 0) {
                int percent = (int) ((permutations / totalPermutations) * 100);
                if (percent % 5 == 0) {
                    int portionDone = percent / 5;
                    String str = "+"
                            + StringUtils.repeat("#", portionDone)
                            + StringUtils.repeat("_", 20 - portionDone)
                            + "+";
                    System.out.println(str);
                }
            }
            Choices choices1 = createChoices(den, delta, 3);
            if (!choices1.isEmpty())
                groups.add(choices1);
        } else {
            for (int i = 0; i < n; i++) {
                List<Den> one = new ArrayList<>();
                one.addAll(den);
                one.add(dens.get(i));
                List<Den> two = new ArrayList<>();
                two.addAll(dens.subList(0, i));
                two.addAll(dens.subList(i + 1, n));
                permutation(one, two, delta);
            }
        }
    }


    private static Choices createChoices(List<Den> dens, int delta, int numberOfGroups) {

        int shootingForSize = getTotalNumberOfScouts(dens) / numberOfGroups;
        Choices choices = new Choices(shootingForSize);

        try {
            List<Den> selectedListOfDens = getDens(dens, delta, numberOfGroups, shootingForSize);

            while (selectedListOfDens != null && numberOfGroups-- > 1) {
                List<Den> secondSelectedListOfDens = getDens(removeDens(dens, selectedListOfDens), delta + 1, numberOfGroups,
                        shootingForSize);

                if (secondSelectedListOfDens != null) {
                    Collections.sort(selectedListOfDens);
                    Collections.sort(secondSelectedListOfDens);

                    List<Den> densMinusSecondPass = new ArrayList<>();
                    for (Den innerDen : dens) {
                        if (!selectedListOfDens.contains(innerDen) && !secondSelectedListOfDens.contains(innerDen)) {
                            densMinusSecondPass.add(innerDen);
                        }
                    }

                    if (getTotalNumberOfScouts(densMinusSecondPass) - shootingForSize < (delta + 1)) {
                        Collections.sort(densMinusSecondPass);
                        choices.add(selectedListOfDens, secondSelectedListOfDens, densMinusSecondPass);
                    }
                }

            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return choices;
    }

    private static List<Den> removeDens(List<Den> dens, List<Den> selectedListOfDens) {
        List<Den> densMinusFirstPass = new ArrayList<>();
        for (Den innerDen : dens) {
            if (!selectedListOfDens.contains(innerDen)) {
                densMinusFirstPass.add(innerDen);
            }
        }
        return densMinusFirstPass;
    }

    private static List<Den> getDens(List<Den> dens, int delta, int numberOfGroups, int shootingForSize) {
        Multimap<Integer, List<Den>> groupsMap = getIntegerListMultimap(dens, numberOfGroups, delta);

        List<Den> selectedListOfDens = null;

        for (Map.Entry<Integer, List<Den>> entry : groupsMap.entries()) {
            if (abs(entry.getKey() - shootingForSize) < delta) {
                selectedListOfDens = entry.getValue();
            }
        }
        return selectedListOfDens;
    }

    private static void ensureDen8WithAnotherWolfPack(Set<Choices> groups) {
        groups.removeIf(choices -> !den8WithAnotherPackWolfPack(choices));
    }

    private static boolean den8WithAnotherPackWolfPack(Choices choices) {
        if (listContainsDen8(choices.denList.get(0))) {
            return choices.denList.get(0).stream().anyMatch(Main::isWolf);
        } else if (listContainsDen8(choices.denList.get(1))) {
            return choices.denList.get(1).stream().anyMatch(Main::isWolf);
        } else if (listContainsDen8(choices.denList.get(2))) {
            return choices.denList.get(2).stream().anyMatch(Main::isWolf);
        }

        return false;
    }

    private static boolean isWolf(Den den) {
        return den.getNumber() != 8 && den.getAnimal().equalsIgnoreCase("Wolf");
    }

    private static boolean listContainsDen8(List<Den> list1) {
        return list1.stream().anyMatch(den -> den.getNumber() == 8);
    }

    /**
     * Return a map with the total number of scouts in the group as the key and the dens in that group as the value.
     * Since there can be more than createChoices group with the same number of total scouts I am using a Multimap.
     *
     * It is possible that with a given delta there will be no results, which is why I am passing it in, so the
     * caller can increase it if no groups are within the original delta.
     */
    private static Multimap<Integer, List<Den>> getIntegerListMultimap(List<Den> dens, int numberOfGroups, int delta) {

        int average = getAverageNumberOfScouts(dens, numberOfGroups);

        Multimap<Integer, List<Den>> groupsMap = ArrayListMultimap.create();

        for (Den den : dens) {
            int numberOfScouts = den.getNumberOfScouts();
            List<Den> denList = new ArrayList<>();
            denList.add(den);
            for (Den den2 : remove(den, dens)) {
                int numberOfScouts2 = den2.getNumberOfScouts();
                numberOfScouts += numberOfScouts2;
                denList.add(den2);
                if (numberOfScouts >= (average - delta)) {
                    if (numberOfScouts <= average + delta)
                        groupsMap.put(numberOfScouts, new ArrayList<>(denList));
                    denList.clear();
                    break;
                }
            }
        }
        return groupsMap;
    }

    private static int getAverageNumberOfScouts(List<Den> dens, int numberOfGroups) {
        int sum = getTotalNumberOfScouts(dens);
        return sum / numberOfGroups;
    }

    private static int getTotalNumberOfScouts(List<Den> dens) {
        return dens.stream().mapToInt(Den::getNumberOfScouts).sum();
    }

    private static List<Den> remove(Den den, List<Den> dens) {
//        dens.remove(den);
//        return dens;
        List<Den> newDens = new ArrayList<>(dens);
        newDens.remove(den);
        return newDens;
    }

    private static class Den implements Comparable<Den> {
        private int number;
        private int numberOfScouts;
        private String animal;

        private Den() {
        }

        static Den of(int number, int numberOfScouts, String animal) {
            Den den = new Den();
            den.setNumber(number);
            den.setNumberOfScouts(numberOfScouts);
            den.setAnimal(animal);
            return den;
        }

        void setNumber(int number) {
            this.number = number;
        }

        int getNumber() {
            return number;
        }

        void setNumberOfScouts(int numberOfScouts) {
            this.numberOfScouts = numberOfScouts;
        }

        int getNumberOfScouts() {
            return numberOfScouts;
        }

        @Override
        public String toString() {
            return "Den " +
                    number
                    + " - " + animal
                    + ":" + numberOfScouts;
        }

        @Override
        public int compareTo(Den o) {
            return this.getNumber() - o.getNumber();
        }

        void setAnimal(String animal) {
            this.animal = animal;
        }

        String getAnimal() {
            return animal;
        }
    }

    private static class Choices implements Comparable<Choices> {
        private final int shootingForSize;
        private DenList denList;

        public Choices(int shootingForSize) {
            this.shootingForSize = shootingForSize;
        }

        void add(List<Den> ... lists) {
            this.denList = new DenList(Arrays.asList(lists));
        }

        @Override
        public String toString() {
            return "\tA=" + getListOutputString(denList.get(0)) +
                    "\n\tB=" + getListOutputString(denList.get(1)) +
                    "\n\tC=" + getListOutputString(denList.get(2));
        }

        private String getListOutputString(List<Den> dens) {
            return String.format("%s - %d different age levels and %d total",
                    dens,
                    numberOfDifferentLevels(dens),
                    getTotalNumberOfScouts(dens));
        }

        private int numberOfDifferentLevels(List<Den> dens) {
            if (dens == null) {
                return 0;
            }
            Set<String> differentLevels = new HashSet<>();
            for (Den den : dens) {
                differentLevels.add(den.getAnimal());
            }
            return differentLevels.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Choices choices = (Choices) o;

            return (this.denList.get(0).equals(choices.denList.get(0))
                    && this.denList.get(1).equals(choices.denList.get(1))
                    && this.denList.get(2).equals(choices.denList.get(2)));
        }

        @Override
        public int hashCode() {
            int result = denList.get(0) != null ? denList.get(0).toString().hashCode() : 0;
            result = 31 * result + (denList.get(1) != null ? denList.get(1).toString().hashCode() : 0);
            result = 31 * result + (denList.get(2) != null ? denList.get(2).toString().hashCode() : 0);
            return result;
        }

        public boolean isEmpty() {
            return denList == null || denList.get(0) == null || denList.get(0).isEmpty();
        }

        @Override
        public int compareTo(Choices o) {
            int delta1 = deltaOf(this);
            int delta2 = deltaOf(o);
            int deltaDiff = delta1 - delta2;
            if (deltaDiff == 0) {
                int differentGroups = totalNumberOfDifferentGroups(this);
                int differentGroups1 = totalNumberOfDifferentGroups(o);
                return differentGroups - differentGroups1;
            }
            return deltaDiff;
        }

        private int totalNumberOfDifferentGroups(Choices choices) {
            return numberOfDifferentLevels(choices.denList.get(0)) +
                    numberOfDifferentLevels(choices.denList.get(1)) +
                    numberOfDifferentLevels(choices.denList.get(2));
        }

        private int deltaOf(Choices o) {
            int i = 2 ^ abs(shootingForSize - getTotalNumberOfScouts(o.denList.get(0)));
            int j = 2 ^ abs(shootingForSize - getTotalNumberOfScouts(o.denList.get(1)));
            int k = 2 ^ abs(shootingForSize - getTotalNumberOfScouts(o.denList.get(2)));
            return i + j + k;
        }

        private class DenList implements Comparable<List<Den>> {
            private final List<List<Den>> lists;

            public DenList(List<List<Den>> lists) {
                this.lists = lists;
            }

            public List<Den> get(int i) {
                return lists.get(i);
            }

            @Override
            public int compareTo(List<Den> o) {
                return 0;
            }
        }
    }
}
