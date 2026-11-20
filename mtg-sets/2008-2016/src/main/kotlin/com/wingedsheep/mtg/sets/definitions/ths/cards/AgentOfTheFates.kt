package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agent of the Fates — Theros #76 (canonical printing)
 * {1}{B}{B}
 * Creature — Human Assassin
 * 3/2
 * Deathtouch
 * Heroic — Whenever you cast a spell that targets this creature, each opponent sacrifices a
 * creature of their choice.
 *
 * Heroic is the `TargetsSource` cast trigger (Fabled Hero); the payoff is Dictate of Erebos's
 * each-opponent edict.
 */
val AgentOfTheFates = card("Agent of the Fates") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 3
    toughness = 2
    oracleText = "Deathtouch\nHeroic — Whenever you cast a spell that targets this creature, each opponent sacrifices a creature of their choice."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Heroic — Whenever you cast a spell that targets this creature, each opponent sacrifices a creature of their choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Matt Stewart"
        flavorText = "\"You are breathing borrowed air.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecd59977-8788-417d-ba50-e21b8a636bd3.jpg?1783939786"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
