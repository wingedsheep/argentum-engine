package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Serpent Society — Marvel Super Heroes #226 (rare)
 * {1}{B}{G} · Legendary Creature — Human Snake Villain · 3/4
 *
 * Deathtouch
 * Ward—Get five poison counters.
 * Whenever another creature you control with deathtouch dies, each opponent sacrifices a nontoken
 * creature of their choice.
 *
 * The ward is [KeywordAbility.wardPlayerCounters] — counters placed on the *paying* player
 * (CR 122.1). It is the one ward cost with no affordability gate: an opponent can always get five
 * poison counters, so the ward never counters for inability, it only asks. Half of a ten-poison
 * loss (CR 122.1f) is a steep but always-available price, which is the card's whole point.
 *
 * The death trigger's "with deathtouch" reads **last-known information**: the creature is already
 * in the graveyard when the trigger is detected, so the filter's keyword predicate is matched
 * against the keywords frozen on the `ZoneChangeEvent` (the Jackdaw Savior path). Deathtouch
 * granted by a continuous effect therefore counts, and a creature that lost deathtouch before
 * dying does not.
 *
 * The payoff is the ordinary multi-player edict — one [Effects.Sacrifice] over
 * [Player.EachOpponent], nontoken-filtered. "Of their choice" is the executor's default, and the
 * effect does only as much as it can (CR 609.3, backed by CR 101.3): an opponent whose only
 * creatures are tokens sacrifices nothing.
 */
val TheSerpentSociety = card("The Serpent Society") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Snake Villain"
    power = 3
    toughness = 4
    oracleText = "Deathtouch\n" +
        "Ward—Get five poison counters. (A player with ten or more poison counters loses the game.)\n" +
        "Whenever another creature you control with deathtouch dies, each opponent sacrifices a " +
        "nontoken creature of their choice."

    keywords(Keyword.DEATHTOUCH)
    keywordAbility(KeywordAbility.wardPlayerCounters(Counters.POISON, 5))

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().withKeyword(Keyword.DEATHTOUCH),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.nontoken(),
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Rimas Valeikis"
        flavorText = "\"Serpents of the world, unite!\"\n—King Cobra"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/209855ee-d531-4b58-926b-8da171d46619.jpg?1783902897"
    }
}
