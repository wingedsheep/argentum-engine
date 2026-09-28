package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Invasion of Regatha // Disciples of the Inferno — March of the Machine #148.
 * {2}{R} · Battle — Siege · defense 5 // Creature — Human Monk 4/4
 *
 * The front's "another target battle or opponent" is [Targets.OpponentOrBattle] wrapped in
 * [TargetOther], which excludes the Siege itself. The back's damage bump is [ModifyDamageAmount]
 * (Valley Flamecaller) scoped to noncreature sources you control and to creature / battle /
 * opponent recipients.
 */
private val InvasionOfRegathaFront = card("Invasion of Regatha") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, it deals 4 damage to another target battle or opponent and 1 " +
        "damage to up to one target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val main = target(TargetOther(Targets.OpponentOrBattle))
        val small = target(TargetFilter.Creature, optional = true)
        effect = Effects.DealDamage(4, main) then Effects.DealDamage(1, small)
        description = "When this Siege enters, it deals 4 damage to another target battle or " +
            "opponent and 1 damage to up to one target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ada79e04-bb69-4c92-a829-014f02b1e06a.jpg?1783916994"
    }
}

private val DisciplesOfTheInferno = card("Disciples of the Inferno") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Human Monk"
    power = 4
    toughness = 4
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until " +
        "end of turn.)\n" +
        "If a noncreature source you control would deal damage to a creature, battle, or " +
        "opponent, it deals that much damage plus 2 instead."

    prowess()

    replacementEffect(
        ModifyDamageAmount(
            modifier = 2,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.AnyOf(
                    listOf(
                        Recipient.AnyCreature,
                        Recipient.Object(GameObjectFilter.Battle),
                        Recipient.Opponent,
                    )
                ),
                source = GameObjectFilter.Noncreature.youControl(),
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/back/a/d/ada79e04-bb69-4c92-a829-014f02b1e06a.jpg?1783916994"
        ruling("2023-04-14", "The additional 2 damage is dealt by the same source as the original source of damage. The damage isn't dealt by Disciples of the Inferno.")
    }
}

val InvasionOfRegatha: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfRegathaFront,
    backFace = DisciplesOfTheInferno,
)
