package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wedding Announcement // Wedding Festivity (Innistrad: Crimson Vow)
 * {2}{W}
 * Enchantment // Enchantment
 *
 * Front — Wedding Announcement
 *   At the beginning of your end step, put an invitation counter on this enchantment. If you
 *   attacked with two or more creatures this turn, draw a card. Otherwise, create a 1/1 white Human
 *   creature token. Then if this enchantment has three or more invitation counters on it, transform it.
 *
 * Back — Wedding Festivity
 *   Creatures you control get +1/+1.
 *
 * The end-step ability is a [Effects.Composite] of three ordered steps:
 *  1. add an invitation counter to itself;
 *  2. a [ConditionalEffect] whose then/else honor "if you attacked with two or more creatures … draw
 *     a card. Otherwise, create a 1/1 white Human token" via [Conditions.YouAttackedWithCreaturesThisTurn];
 *  3. a second [ConditionalEffect] gated on [Conditions.SourceCounterCountAtLeast] 3, transforming it
 *     (Treasure Map's counter-then-transform idiom). The back is a transformed face with no mana
 *     cost, so its color comes from a color indicator (CR 204): `colorIndicator = "W"`.
 */

private val WeddingAnnouncementFront = card("Wedding Announcement") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, put an invitation counter on this enchantment. " +
        "If you attacked with two or more creatures this turn, draw a card. Otherwise, create a 1/1 " +
        "white Human creature token. Then if this enchantment has three or more invitation counters " +
        "on it, transform it."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.Composite(
            Effects.AddCounters("invitation", 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.YouAttackedWithCreaturesThisTurn(GameObjectFilter.Creature, atLeast = 2),
                effect = Effects.DrawCards(1),
                elseEffect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Human"),
                    imageUri = "https://cards.scryfall.io/normal/front/7/d/7d13a93a-a43d-4cf5-8300-8341f3b7f1b1.jpg?1783924701",
                ),
            ),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast("invitation", 3),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "At the beginning of your end step, put an invitation counter on this " +
            "enchantment. If you attacked with two or more creatures this turn, draw a card. " +
            "Otherwise, create a 1/1 white Human creature token. Then if this enchantment has three " +
            "or more invitation counters on it, transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c3ddb1f-a1de-4fea-9042-5e9caa16ceb2.jpg?1783924910"
    }
}

private val WeddingFestivity = card("Wedding Festivity") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W" // Transformed back face, no mana cost (CR 204).
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+1."

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/back/2/c/2c3ddb1f-a1de-4fea-9042-5e9caa16ceb2.jpg?1783924910"
    }
}

val WeddingAnnouncement: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = WeddingAnnouncementFront,
    backFace = WeddingFestivity,
)
