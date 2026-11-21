package com.wingedsheep.mtg.sets.definitions.ddl.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Polukranos, World Eater — Duel Decks: Heroes vs. Monsters #43 (canonical printing; reprinted in
 * Theros)
 * {2}{G}{G} · Legendary Creature — Hydra · 5/5
 *
 * {X}{X}{G}: Monstrosity X.
 * When Polukranos becomes monstrous, it deals X damage divided as you choose among any number of
 * target creatures your opponents control. Each of those creatures deals damage equal to its power
 * to Polukranos.
 *
 * The trigger's X is the X of the monstrosity activation (Hydra Broodmaster's `xValue`). Targets are
 * "any number", capped at X because each must be assigned at least 1 (so X = 0 means no targets);
 * the division is announced as the trigger goes on the stack (Ureni's triggered DividedDamage). Then
 * each still-legal target deals damage equal to its power to Polukranos, each creature as its own
 * source (`damageSource = IterationEntity`) — after Polukranos's damage, per the ruling, and before
 * state-based actions, so a creature dealt lethal damage still hits back. An illegal target is
 * neither damaged nor damages Polukranos.
 */
val PolukranosWorldEater = card("Polukranos, World Eater") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Hydra"
    power = 5
    toughness = 5
    oracleText = "{X}{X}{G}: Monstrosity X. (If this creature isn't monstrous, put X +1/+1 counters on it and it becomes monstrous.)\n" +
        "When Polukranos becomes monstrous, it deals X damage divided as you choose among any number of target " +
        "creatures your opponents control. Each of those creatures deals damage equal to its power to Polukranos."

    activatedAbility {
        cost = Costs.Mana("{X}{X}{G}")
        effect = Effects.Monstrosity(DynamicAmounts.xValue())
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        val victims = targets(
            TargetFilter.CreatureOpponentControls,
            minCount = 0,
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        effect = Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue()) then
            Effects.ForEachTargetOf(
                victims,
                Effects.DealDamage(
                    amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                    target = EffectTarget.Self,
                    damageSource = EffectTarget.IterationEntity,
                )
            )
        description = "When Polukranos becomes monstrous, it deals X damage divided as you choose among any number " +
            "of target creatures your opponents control. Each of those creatures deals damage equal to its power " +
            "to Polukranos."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "43"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b5a1182-c707-4024-9000-de57793fba0d.jpg?1783939843"
        ruling("2013-09-15", "The value of X in Polukranos's last ability is equal to the value chosen for X when its activated ability was activated.")
        ruling("2013-09-15", "You choose the division of damage as you put the ability on the stack, not as it resolves. Each target must be assigned at least 1 damage. In multiplayer games, you may choose creatures controlled by different opponents.")
        ruling("2013-09-15", "If some, but not all, of the ability's targets become illegal, you can't change the division of damage. Damage that would've been dealt to illegal targets simply isn't dealt.")
        ruling("2013-09-15", "As Polukranos's triggered ability resolves, Polukranos deals damage first, then the target creatures do. Although no creature will die until after the ability finishes resolving, the order could matter if Polukranos has wither or infect.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
