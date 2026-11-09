package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hanweir Militia Captain // Westvale Cult Leader (Shadows over Innistrad #21)
 * {1}{W}
 * Creature — Human Soldier 2/2 // Creature — Human Cleric * / *
 *
 * Front — "At the beginning of your upkeep, if you control four or more creatures, transform this
 *         creature."
 * Back  — "Westvale Cult Leader's power and toughness are each equal to the number of creatures you
 *         control. At the beginning of your end step, create a 1/1 white and black Human Cleric
 *         creature token."
 *
 * The four-creature check is an intervening "if" (CR 603.4): checked as the upkeep begins and again
 * on resolution, so it rides on `interveningIf` rather than an `Effects.If` inside the effect. The
 * count includes the Captain itself. Once transformed, falling below four creatures never flips it
 * back — nothing on the back face watches the count.
 *
 * The back face has no mana cost, so its white comes from a color indicator (CR 204).
 */

private val HanweirMilitiaCaptainFront = card("Hanweir Militia Captain") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your upkeep, if you control four or more creatures, transform this creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControlAtLeast(4, GameObjectFilter.Creature)
        effect = Effects.Transform(EffectTarget.Self)
        description = "At the beginning of your upkeep, if you control four or more creatures, transform this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "David Palumbo"
        flavorText = "Her reputation inspires both loyalty and adoration."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e137566-5033-48e8-88ba-65e3821b3cec.jpg?1783937824"
        ruling(
            "2016-04-08",
            "If you don't control four or more creatures (including Hanweir Militia Captain itself) at " +
                "the moment your upkeep begins, its ability doesn't trigger. You can't wait for other " +
                "effects in your upkeep to provide creatures."
        )
        ruling(
            "2016-04-08",
            "If you don't control four or more creatures as Hanweir Militia Captain's ability resolves, " +
                "it won't transform."
        )
        ruling(
            "2016-04-08",
            "Once Hanweir Militia Captain has transformed into Westvale Cult Leader, controlling fewer " +
                "than four creatures won't cause it to transform back."
        )
    }
}

private val WestvaleCultLeader = card("Westvale Cult Leader") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Cleric"
    oracleText = "Westvale Cult Leader's power and toughness are each equal to the number of creatures you control.\n" +
        "At the beginning of your end step, create a 1/1 white and black Human Cleric creature token."
    dynamicStats(DynamicAmounts.creaturesYouControl())

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Human", "Cleric"),
            imageUri = "https://cards.scryfall.io/normal/front/9/4/94ed2eca-1579-411d-af6f-c7359c65de30.jpg?1783937680",
        )
        description = "At the beginning of your end step, create a 1/1 white and black Human Cleric creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/back/7/e/7e137566-5033-48e8-88ba-65e3821b3cec.jpg?1783937824"
        ruling(
            "2016-04-08",
            "Westvale Cult Leader's first ability counts itself, so it will normally be at least 1/1."
        )
    }
}

val HanweirMilitiaCaptain: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HanweirMilitiaCaptainFront,
    backFace = WestvaleCultLeader,
)
