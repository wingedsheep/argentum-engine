package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Great Ugly-Looking Goblin // Clap! Snap! — The Hobbit #74
 * {5}{B}
 * Creature — Goblin Soldier
 * 4/4
 *
 * Each creature you control with a +1/+1 counter on it has menace.
 *
 * Adventure: Clap! Snap! — {1}{B}, Sorcery — Adventure
 * Amass Goblins 2.
 *
 * The menace clause is a plain Layer 6 keyword grant over the counter-gated group (Training Regimen),
 * so it tracks counters arriving and leaving continuously rather than being snapshotted. "Each
 * creature you control" includes the Goblin itself once it carries a counter, so the group is *not*
 * `excludeSelf` — and the Army minted by the Adventure qualifies the moment it gets its counters.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val GreatUglyLookingGoblin = card("Great Ugly-Looking Goblin") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Goblin Soldier"
    power = 4
    toughness = 4
    oracleText = "Each creature you control with a +1/+1 counter on it has menace. " +
        "(It can't be blocked except by two or more creatures.)"

    staticAbility {
        ability = GrantKeyword(
            Keyword.MENACE,
            GroupFilter(GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE)),
        )
    }

    adventure("Clap! Snap!") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Amass Goblins 2. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Amass(2, "Goblin")
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c87f6004-e1cf-42b2-9647-322bc4939339.jpg?1785237962"
    }
}
