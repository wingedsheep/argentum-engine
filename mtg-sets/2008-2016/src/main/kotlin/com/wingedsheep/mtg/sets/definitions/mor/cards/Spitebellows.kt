package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spitebellows
 * {5}{R}
 * Creature — Elemental
 * 6/1
 * When this creature leaves the battlefield, it deals 6 damage to target creature.
 * Evoke {1}{R}{R} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)
 *
 * Evoke is the first-class [card] field `evoke` (cf. Mulldrifter) — the engine supplies the
 * "sacrificed when it enters" trigger itself, so none is written here. The damage rider is a
 * [Triggers.LeavesBattlefield] trigger (not a dies trigger — it fires on exile and bounce too)
 * over [Targets.Creature] with a fixed [Effects.DealDamage] of 6 on [EffectTarget.ContextTarget];
 * the source is left implicit, so the engine attributes the damage to this creature's last known
 * information, which is the only thing left of it by the time the ability resolves.
 */
val Spitebellows = card("Spitebellows") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 1
    oracleText = "When this creature leaves the battlefield, it deals 6 damage to target creature.\n" +
        "Evoke {1}{R}{R} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)"

    evoke = "{1}{R}{R}"

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        target = Targets.Creature
        effect = Effects.DealDamage(6, EffectTarget.ContextTarget(0))
        description = "When this creature leaves the battlefield, it deals 6 damage to target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Larry MacDougall"
        flavorText = "Disaster stalks with gaping jaws across unready lands."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43f2104d-aeff-493f-8227-cb95bf3e2eab.jpg"
        ruling("2008-04-01", "Evoke doesn't change the timing of when you can cast the creature that has it. If you could cast that creature spell only when you could cast a sorcery, the same is true for cast it with evoke.")
        ruling("2008-04-01", "If a creature spell cast with evoke changes controllers before it enters, it will still be sacrificed when it enters. Similarly, if a creature cast with evoke changes controllers after it enters but before its sacrifice ability resolves, it will still be sacrificed. In both cases, the controller of the creature at the time it left the battlefield will control its leaves-the-battlefield ability.")
        ruling("2008-04-01", "When you cast a spell by paying its evoke cost, its mana cost doesn't change. You just pay the evoke cost instead.")
        ruling("2008-04-01", "Effects that cause you to pay more or less to cast a spell will cause you to pay that much more or less while casting it for its evoke cost, too. That's because they affect the total cost of the spell, not its mana cost.")
        ruling("2008-04-01", "Whether evoke's sacrifice ability triggers when the creature enters depends on whether the spell's controller chose to pay the evoke cost, not whether they actually paid it (if it was reduced or otherwise altered by another ability, for example).")
        ruling("2008-04-01", "If you're casting a spell \"without paying its mana cost,\" you can't use its evoke ability.")
    }
}
