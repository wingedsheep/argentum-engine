package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Haunted Hellride
 * {1}{U}{B}
 * Artifact — Vehicle
 * 3/3
 * Whenever you attack, target creature you control gets +1/+0 and gains deathtouch until end of
 * turn. Untap it.
 * Crew 1
 *
 * The attack trigger is the once-per-combat group trigger ([Triggers.YouAttack]), not an "attacks"
 * trigger on this permanent — it fires whenever you declare any attacker, even when this Vehicle
 * isn't among them (or isn't a creature at all). "Untap it" refers back to the same target, so all
 * three parts share one target requirement.
 */
val HauntedHellride = card("Haunted Hellride") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Artifact — Vehicle"
    oracleText = "Whenever you attack, target creature you control gets +1/+0 and gains deathtouch " +
        "until end of turn. Untap it.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YouAttack
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            Effects.ModifyStats(1, 0, creature),
            Effects.GrantKeyword(Keyword.DEATHTOUCH, creature),
            Effects.Untap(creature)
        )
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2ce0d37-d5d7-49dd-885e-9998bb8abede.jpg?1783907857"
    }
}
