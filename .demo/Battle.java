package Battle;

import Battle.SubHandler.HeroActionHandler;
import Battle.SubHandler.MonsterActionHandler;
import Battle.Reward.RewardStrategy;
import Battle.Reward.DefaultRewardStrategy;
import Heros.Hero;
import Monster.AbstractMonster_DSE;
import Utilize.ColorUtil;
import Game.Game;
import java.util.*;

/**
 * Battle class controls the core loop for turn-based combat.
 * Dynamically builds turn order based on alive heroes and monsters.
 */
public class Battle {
    private final List<Hero> heroParty;
    private final List<AbstractMonster_DSE> enemyGroup;
    private final Scanner inputScanner = new Scanner(System.in);
    private final Game game;
    private RewardStrategy rewardStrategy = new DefaultRewardStrategy();
    public Battle(List<Hero> heroes, List<AbstractMonster_DSE> monsters, Game game) {
        this.heroParty = heroes;
        this.enemyGroup = monsters;
        this.game = game;
    }

    public void startBattle() {
        while (true) {
            for (Hero h : heroParty) {
                if (h.getHP() > 0) {
                    new HeroActionHandler(h,heroParty, enemyGroup, inputScanner).takeTurn();
                    if (checkBattleEnded()) return;
                }
            }

            for (AbstractMonster_DSE m : enemyGroup) {
                if (m.getHP() > 0) {
                    new MonsterActionHandler(m, heroParty).takeTurn();
                    if (checkBattleEnded()) return;
                }
            }
        }
    }
    /**
     * Check whether this battle ends.
     */

    private boolean checkBattleEnded() {
        boolean heroesDefeated = heroParty.stream().allMatch(h -> h.getHP() <= 0);
        boolean monstersDefeated = enemyGroup.stream().allMatch(m -> m.getHP() <= 0);

        if (heroesDefeated) {
            System.out.println(ColorUtil.red("All heroes are defeated. Game Over."));
            game.setGameover();
            return true;
        }

        if (monstersDefeated) {
            System.out.println(ColorUtil.green("Heroes are victorious!"));
            applyVictoryRewards();
            return true;
        }

        return false;
    }
    /**
     * Rewards heroes after winning a battle with experience and gold.
     */
    private void applyVictoryRewards() {
        rewardStrategy.apply(heroParty, enemyGroup);
    }
}
