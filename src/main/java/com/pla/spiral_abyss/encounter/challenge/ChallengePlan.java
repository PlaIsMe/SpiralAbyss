package com.pla.spiral_abyss.encounter.challenge;

import com.pla.spiral_abyss.encounter.challenge.Challenge.*;
import java.util.*;
import java.util.function.Predicate;

/** Coordinated six-floor policy. Donor objectives and their copies are selected atomically. */
public final class ChallengePlan {
    public static final int VERSION = 5;
    private static final List<Type> EARLY_HARD = List.of(Type.WEAPON, Type.PROJECTILE, Type.EXPLOSION,
            Type.PLAYER_BURNING, Type.MOB_BURNING, Type.PLAYER_EFFECT, Type.MOB_EFFECT, Type.NO_ARMOR,
            Type.TAKE_DAMAGE, Type.FISHING, Type.TOTEM, Type.TRICKSHOT, Type.ENDER_PEARL_HIT,
            Type.SHIELD_BLOCK);
    private static final List<Type> LATE_HARD = List.of(Type.ARMOR_ENCHANTMENT, Type.WEAPON_ENCHANTMENT);
    public record Context(List<Integer> populations, Predicate<Challenge> eligible) {
        public Context {
            populations = List.copyOf(populations);
            if (populations.isEmpty() || populations.stream().anyMatch(count -> count < 1)) throw new IllegalArgumentException("Missing wave populations");
        }
        boolean accepts(List<Challenge> selected) {
            return selected.stream().allMatch(eligible) && populations.stream().allMatch(count -> ChallengeCatalog.feasible(selected, count));
        }
    }
    private ChallengePlan() {}
    public static Challenge mediumCopy(Challenge source) {
        return new Challenge(source.type(), Difficulty.MEDIUM, source.target(), source.percentage(), source.ids(), source.forbiddenSlots());
    }
    public static Map<Integer,List<Challenge>> select(long seed, ChallengeCatalog.Snapshot catalog,
            Map<Integer,List<Challenge>> saved, Map<Integer,Context> contexts) {
        Map<Integer,List<Challenge>> result = new TreeMap<>();
        // TIME is policy-mandated, so an explicitly disabled tier cannot be bypassed by saved data.
        for (Difficulty difficulty : Difficulty.values()) if (catalog.tier(Type.TIME,difficulty) == null)
            throw new IllegalArgumentException("The floor challenge policy requires enabled time " + difficulty.key());
        if (validSaved(saved,contexts)) return saved;
        for (int floor=1; floor<=6; floor++) {
            if (!contexts.containsKey(floor)) throw new IllegalArgumentException("Missing challenge context for floor " + floor);
            List<Challenge> fixed = new ArrayList<>();
            fixed.add(time(floor,Difficulty.EASY,seed,catalog,saved));
            if (floor<=4) fixed.add(time(floor,Difficulty.MEDIUM,seed,catalog,saved));
            if (floor<=2) fixed.add(time(floor,Difficulty.HARD,seed,catalog,saved));
            if (!contexts.get(floor).accepts(fixed)) throw new IllegalArgumentException("Required time challenges are unavailable");
            result.put(floor,fixed);
        }
        Map<Integer,List<Challenge>> options = new HashMap<>();
        for (int floor=3; floor<=6; floor++) options.put(floor,options(floor,floor<=4?EARLY_HARD:LATE_HARD,seed,catalog,saved));
        if (!search(3,result,options,contexts,new int[]{100000})) throw new IllegalArgumentException("No feasible distinct floor 3/4 and floor 5/6 challenge plan");
        Map<Integer,List<Challenge>> immutable = new TreeMap<>(); result.forEach((floor,list)->immutable.put(floor,List.copyOf(list)));
        return Collections.unmodifiableMap(immutable);
    }
    public static List<Challenge> timeOnly(int floor,long seed,ChallengeCatalog.Snapshot catalog,Map<Integer,List<Challenge>> saved) {
        List<Challenge> result=new ArrayList<>();
        for(Difficulty difficulty:Difficulty.values()) {
            if(catalog.tier(Type.TIME,difficulty)==null)throw new IllegalArgumentException("Missing required time " + difficulty.key());
            result.add(time(floor,difficulty,seed,catalog,saved));
        }
        return List.copyOf(result);
    }
    private static boolean validSaved(Map<Integer,List<Challenge>> saved,Map<Integer,Context> contexts) {
        for(int floor=1;floor<=6;floor++) {
            var list=saved.get(floor);
            if(list==null||list.size()!=3||!contexts.containsKey(floor)||!contexts.get(floor).accepts(list)||!list.stream().allMatch(ChallengeCatalog::available))return false;
            for(int slot=0;slot<3;slot++)if(list.get(slot).difficulty()!=Difficulty.values()[slot])return false;
            if(list.get(0).type()!=Type.TIME||floor<=4&&list.get(1).type()!=Type.TIME||floor<=2&&list.get(2).type()!=Type.TIME)return false;
            if(floor==3||floor==4){if(!EARLY_HARD.contains(list.get(2).type()))return false;}
            if(floor>=5){if(!LATE_HARD.contains(list.get(2).type())||!list.get(1).equals(mediumCopy(saved.get(floor-2).get(2))))return false;}
        }
        return saved.get(3).get(2).type()!=saved.get(4).get(2).type()&&saved.get(5).get(2).type()!=saved.get(6).get(2).type();
    }
    private static Challenge time(int floor,Difficulty difficulty,long seed,ChallengeCatalog.Snapshot catalog,Map<Integer,List<Challenge>> saved) {
        Challenge old = saved.getOrDefault(floor,List.of()).stream().filter(c->c.difficulty()==difficulty&&c.type()==Type.TIME).findFirst().orElse(null);
        var choices=catalog.candidates(Type.TIME,difficulty,seed^floor^difficulty.ordinal(),old);
        if(choices.isEmpty()) throw new IllegalArgumentException("Missing required time challenge");
        return choices.get(0);
    }
    private static List<Challenge> options(int floor,List<Type> allowed,long seed,ChallengeCatalog.Snapshot catalog,Map<Integer,List<Challenge>> saved) {
        Challenge old=saved.getOrDefault(floor,List.of()).stream().filter(c->c.difficulty()==Difficulty.HARD&&allowed.contains(c.type())).findFirst().orElse(null);
        List<Type> types=new ArrayList<>(allowed); Collections.shuffle(types,new Random(seed^floor));
        if(old!=null){types.remove(old.type());types.add(0,old.type());}
        List<Challenge> result=new ArrayList<>();
        for(Type type:types) result.addAll(catalog.candidates(type,Difficulty.HARD,seed^floor^((long)type.ordinal()<<32),old!=null&&old.type()==type?old:null));
        return result;
    }
    private static boolean search(int floor,Map<Integer,List<Challenge>> result,Map<Integer,List<Challenge>> options,Map<Integer,Context> contexts,int[] budget) {
        if(floor==7)return true;
        for(Challenge candidate:options.get(floor)) {
            if(--budget[0]<0)return false;
            if(floor==4&&candidate.type()==result.get(3).get(2).type())continue;
            if(floor==6&&candidate.type()==result.get(5).get(2).type())continue;
            List<Challenge> selected=result.get(floor); selected.add(candidate);
            if(floor<=4)result.get(floor+2).add(mediumCopy(candidate));
            boolean valid=contexts.get(floor).accepts(selected)
                    &&(floor>4||contexts.get(floor+2).accepts(result.get(floor+2)));
            if(valid&&search(floor+1,result,options,contexts,budget))return true;
            if(floor<=4)result.get(floor+2).remove(1);
            selected.remove(selected.size()-1);
        }
        return false;
    }
}
