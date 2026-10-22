package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spellrune Painter // Spellrune Howler (Innistrad: Midnight Hunt)
 * {2}{R}
 * Creature — Human Shaman Werewolf // Creature — Werewolf
 *
 * Front — Spellrune Painter (2/3): "Whenever you cast an instant or sorcery spell, this creature
 *          gets +1/+1 until end of turn." Daybound.
 * Back  — Spellrune Howler (3/4): the same trigger, but +2/+2. Nightbound.
 */

private val SpellrunePainterFront = card("Spellrune Painter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman Werewolf"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast an instant or sorcery spell, this creature gets +1/+1 until end of turn.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2a5b43d-e21b-4294-9ea2-5bd0264e71d3.jpg?1783925597"
    }
}

private val SpellruneHowler = card("Spellrune Howler") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 3
    toughness = 4
    oracleText = "Whenever you cast an instant or sorcery spell, this creature gets +2/+2 until end of turn.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/back/d/2/d2a5b43d-e21b-4294-9ea2-5bd0264e71d3.jpg?1783925597"
    }
}

val SpellrunePainter: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SpellrunePainterFront,
    backFace = SpellruneHowler,
)
