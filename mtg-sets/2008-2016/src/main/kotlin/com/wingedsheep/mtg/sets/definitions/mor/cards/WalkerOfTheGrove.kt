package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Walker of the Grove
 * {6}{G}{G}
 * Creature — Elemental
 * 7/7
 * When this creature leaves the battlefield, create a 4/4 green Elemental creature token.
 * Evoke {4}{G} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)
 *
 * Evoke is the first-class [card] field `evoke` (cf. Mulldrifter) — the engine supplies the
 * "sacrificed when it enters" trigger itself, so none is written here. The token rider is a
 * [Triggers.LeavesBattlefield] trigger (not a dies trigger — it also fires on exile and bounce)
 * whose effect is a single [Effects.CreateToken] at the default count of one.
 */
val WalkerOfTheGrove = card("Walker of the Grove") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 7
    toughness = 7
    oracleText = "When this creature leaves the battlefield, create a 4/4 green Elemental creature token.\n" +
        "Evoke {4}{G} (You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters.)"

    evoke = "{4}{G}"

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elemental")
        )
        description = "When this creature leaves the battlefield, create a 4/4 green Elemental creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2675b8b-a321-4982-b1d9-5c55d27b9bfd.jpg"
        ruling("2008-04-01", "Evoke doesn't change the timing of when you can cast the creature that has it. If you could cast that creature spell only when you could cast a sorcery, the same is true for cast it with evoke.")
        ruling("2008-04-01", "If a creature spell cast with evoke changes controllers before it enters, it will still be sacrificed when it enters. Similarly, if a creature cast with evoke changes controllers after it enters but before its sacrifice ability resolves, it will still be sacrificed. In both cases, the controller of the creature at the time it left the battlefield will control its leaves-the-battlefield ability.")
        ruling("2008-04-01", "When you cast a spell by paying its evoke cost, its mana cost doesn't change. You just pay the evoke cost instead.")
        ruling("2008-04-01", "Effects that cause you to pay more or less to cast a spell will cause you to pay that much more or less while casting it for its evoke cost, too. That's because they affect the total cost of the spell, not its mana cost.")
        ruling("2008-04-01", "Whether evoke's sacrifice ability triggers when the creature enters depends on whether the spell's controller chose to pay the evoke cost, not whether they actually paid it (if it was reduced or otherwise altered by another ability, for example).")
        ruling("2008-04-01", "If you're casting a spell \"without paying its mana cost,\" you can't use its evoke ability.")
    }
}
