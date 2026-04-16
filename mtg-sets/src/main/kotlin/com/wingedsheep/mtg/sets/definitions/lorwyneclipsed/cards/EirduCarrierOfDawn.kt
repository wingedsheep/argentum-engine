package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eirdu, Carrier of Dawn // Isilu, Carrier of Twilight
 * {3}{W}{W}
 * Legendary Creature — Elemental God (transform)
 * 5/5
 *
 * Front — Eirdu, Carrier of Dawn
 *   Flying, lifelink
 *   Creature spells you cast have convoke.
 *   At the beginning of your first main phase, you may pay {B}. If you do, transform Eirdu.
 *
 * Back — Isilu, Carrier of Twilight
 *   Flying, lifelink
 *   Each other nontoken creature you control has persist.
 *   At the beginning of your first main phase, you may pay {W}. If you do, transform Isilu.
 */
private val IsiluCarrierOfTwilight = card("Isilu, Carrier of Twilight") {
    manaCost = ""
    typeLine = "Legendary Creature — Elemental God"
    oracleText = "Flying, lifelink\n" +
        "Each other nontoken creature you control has persist. (When it dies, if it had " +
        "no -1/-1 counters on it, return it to the battlefield under its owner's control " +
        "with a -1/-1 counter on it.)\n" +
        "At the beginning of your first main phase, you may pay {W}. If you do, transform Isilu."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.PERSIST,
            filter = GroupFilter(
                baseFilter = GameObjectFilter.Creature.youControl().nontoken(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{W}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "13"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/back/b/2/b2d9d5ca-7e15-437a-bdfc-5972b42148fe.jpg?1759144812"
    }
}

private val EirduFrontFace = card("Eirdu, Carrier of Dawn") {
    manaCost = "{3}{W}{W}"
    typeLine = "Legendary Creature — Elemental God"
    oracleText = "Flying, lifelink\n" +
        "Creature spells you cast have convoke. (Your creatures can help cast those spells. " +
        "Each creature you tap while casting a creature spell pays for {1} or one mana of " +
        "that creature's color.)\n" +
        "At the beginning of your first main phase, you may pay {B}. If you do, transform Eirdu."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CONVOKE,
            spellFilter = GameObjectFilter.Creature
        )
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{B}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "13"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2d9d5ca-7e15-437a-bdfc-5972b42148fe.jpg?1759144812"
    }
}

val EirduCarrierOfDawn: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = EirduFrontFace,
    backFace = IsiluCarrierOfTwilight
)
