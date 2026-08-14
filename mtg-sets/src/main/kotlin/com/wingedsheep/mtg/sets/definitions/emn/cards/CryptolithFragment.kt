package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val CryptolithFragmentFront = card("Cryptolith Fragment") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{T}: Add one mana of any color. Each player loses 1 life.\n" +
        "At the beginning of your upkeep, if each player has 10 or less life, transform this artifact."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            Effects.AddManaOfChoice(),
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.Each)),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = Conditions.EachPlayerLifeAtMost(10)
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/078b2103-15ce-456d-b092-352fa7222935.jpg?1783937432"
        ruling("2025-01-24", "Cryptolith Fragment's second ability is a mana ability. Players can't respond to it or to the loss of life it causes.")
        ruling("2025-01-24", "If any player has 11 or more life as your upkeep begins, Cryptolith Fragment's last ability doesn't trigger. If any player has 11 or more life as the ability resolves, the ability has no effect.")
    }
}

private val AuroraOfEmrakul = card("Aurora of Emrakul") {
    manaCost = ""
    typeLine = "Creature — Eldrazi Reflection"
    oracleText = "Flying, deathtouch\nWhenever this creature attacks, each opponent loses 3 life."
    power = 1
    toughness = 4
    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "John Avon"
        flavorText = "\"I felt compelled to take the twisted stone, and I abandoned my horse's burden to accommodate its weight. Now, its continued glow illuminates my home and warms my mind.\"\n—Garner Kroft, Moorland farmer"
        imageUri = "https://cards.scryfall.io/normal/back/0/7/078b2103-15ce-456d-b092-352fa7222935.jpg?1783937432"
    }
}

val CryptolithFragment: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = CryptolithFragmentFront,
    backFace = AuroraOfEmrakul,
)
