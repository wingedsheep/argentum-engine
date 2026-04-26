package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brigid, Clachan's Heart // Brigid, Doun's Mind
 * {2}{W}
 * Legendary Creature — Kithkin Warrior // Legendary Creature — Kithkin Soldier
 * 3/2
 *
 * Front — Brigid, Clachan's Heart
 *   Whenever this creature enters or transforms into Brigid, Clachan's Heart, create a 1/1
 *   green and white Kithkin creature token.
 *   At the beginning of your first main phase, you may pay {G}. If you do, transform Brigid.
 *
 * Back — Brigid, Doun's Mind
 *   {T}: Add X {G} or X {W}, where X is the number of other creatures you control.
 *   At the beginning of your first main phase, you may pay {W}. If you do, transform Brigid.
 */
private val createKithkinToken = CreateTokenEffect(
    power = 1,
    toughness = 1,
    colors = setOf(Color.GREEN, Color.WHITE),
    creatureTypes = setOf("Kithkin"),
    imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
)

private val BrigidDounsMind = card("Brigid, Doun's Mind") {
    manaCost = ""
    typeLine = "Legendary Creature — Kithkin Soldier"
    power = 3
    toughness = 2
    oracleText = "{T}: Add X {G} or X {W}, where X is the number of other creatures you control.\n" +
        "At the beginning of your first main phase, you may pay {W}. If you do, transform Brigid."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, DynamicAmounts.otherCreaturesYouControl())
        manaAbility = true
        description = "{T}: Add X {G}, where X is the number of other creatures you control."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE, DynamicAmounts.otherCreaturesYouControl())
        manaAbility = true
        description = "{T}: Add X {W}, where X is the number of other creatures you control."
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{W}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/back/c/b/cb7d5bbb-4f68-4e38-8bb0-a95af21b24c8.jpg?1767887768"
    }
}

private val BrigidClachansHeartFrontFace = card("Brigid, Clachan's Heart") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Kithkin Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature enters or transforms into Brigid, Clachan's Heart, create a 1/1 " +
        "green and white Kithkin creature token.\n" +
        "At the beginning of your first main phase, you may pay {G}. If you do, transform Brigid."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = createKithkinToken
    }

    triggeredAbility {
        trigger = Triggers.TransformsToFront
        effect = createKithkinToken
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{G}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb7d5bbb-4f68-4e38-8bb0-a95af21b24c8.jpg?1767887768"
    }
}

val BrigidClachansHeart: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BrigidClachansHeartFrontFace,
    backFace = BrigidDounsMind
)
