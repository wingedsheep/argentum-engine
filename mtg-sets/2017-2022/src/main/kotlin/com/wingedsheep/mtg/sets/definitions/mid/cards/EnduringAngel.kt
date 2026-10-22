package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.ReplaceLifeReductionToZero
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.times

/** Enduring Angel // Angelic Enforcer (Innistrad: Midnight Hunt). */
private val EnduringAngelFront = card("Enduring Angel") {
    manaCost = "{2}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying, double strike\nYou have hexproof.\nIf your life total would be reduced to 0 or less, " +
        "instead transform this creature and your life total becomes 3. Then if this creature didn't transform " +
        "this way, you lose the game."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING, Keyword.DOUBLE_STRIKE)

    staticAbility {
        ability = GrantHexproofToController
    }

    // A permanent that isn't double-faced (a single-faced token or copy) can't transform (CR 701.27c):
    // its life total still becomes 3, then its controller loses the game.
    replacementEffect(
        ReplaceLifeReductionToZero(
            instead = Effects.IfYouDo(
                action = Effects.Transform(EffectTarget.Self),
                then = Effects.SetLifeTotal(3),
                otherwise = Effects.SetLifeTotal(3) then Effects.LoseGame(EffectTarget.Controller),
                successCriterion = SuccessCriterion.Transformed,
                descriptionOverride = "transform this creature and your life total becomes 3. " +
                    "Then if this creature didn't transform this way, you lose the game"
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a204c2a3-a899-4b70-8825-7e085b655ed0.jpg?1783925664"
    }
}

private val AngelicEnforcer = card("Angelic Enforcer") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\nYou have hexproof.\nAngelic Enforcer's power and toughness are each equal to your life total.\n" +
        "Whenever this creature attacks, double your life total."
    dynamicStats(DynamicAmounts.yourLifeTotal())

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantHexproofToController
    }

    // CR 701.10d — doubling a life total gains or loses whatever ends it at twice its current value.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.SetLifeTotal(DynamicAmounts.yourLifeTotal() * 2)
        description = "Double your life total."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/back/a/2/a204c2a3-a899-4b70-8825-7e085b655ed0.jpg?1783925664"
    }
}

val EnduringAngel: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = EnduringAngelFront,
    backFace = AngelicEnforcer,
)
