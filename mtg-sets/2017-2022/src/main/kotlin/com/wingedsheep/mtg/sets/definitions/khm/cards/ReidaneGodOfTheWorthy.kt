package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reidane, God of the Worthy // Valkmira, Protector's Shield — Kaldheim #21 (canonical printing)
 * Legendary Creature — God 2/3 // Legendary Artifact · Modal double-faced card
 *
 * Back: Valkmira's prevention reads "you or a permanent you control" (itself included), while its
 * trigger reads "you or **another** permanent you control" — the player half is `players =
 * Player.You`, the object half the `another` binding. "Counter that spell or ability unless its
 * controller pays {1}" is the ward keyword's own counter, aimed at the targeting source.
 */
private val ReidaneGodOfTheWorthyFront = card("Reidane, God of the Worthy") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — God"
    power = 2
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "Snow lands your opponents control enter tapped.\n" +
        "Noncreature spells your opponents cast with mana value 4 or greater cost {2} more to cast."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Land.snow().opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(GameObjectFilter.Noncreature.manaValueAtLeast(4)),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3606519e-5677-4c21-a34e-be195b6669fa.jpg?1783928288"
        ruling("2021-02-05", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases (such as that of Reidane, God of the Worthy), then apply any cost reductions. The mana value of the spell is determined only by its mana cost, no matter what the total cost to cast the spell was.")
        ruling("2021-02-05", "If a spell has {X} in its mana cost, use the value of X to determine whether its mana value is 4 or greater. If it is, Reidane's last ability will apply. If not, it won't.")
    }
}

private val ValkmiraProtectorsShieldBack = card("Valkmira, Protector's Shield") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact"
    oracleText = "If a source an opponent controls would deal damage to you or a permanent you control, " +
        "prevent 1 of that damage.\n" +
        "Whenever you or another permanent you control becomes the target of a spell or ability an " +
        "opponent controls, counter that spell or ability unless its controller pays {1}."

    replacementEffect(
        PreventDamage(
            amount = 1,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.AnyOf(
                    listOf(
                        Recipient.You,
                        Recipient.Object(GameObjectFilter.Permanent.youControl()),
                    )
                ),
                source = GameObjectFilter.Any.opponentControls(),
            ),
        )
    )

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.youControl())
            .becomesTarget(byOpponent = true, players = Player.You)
        effect = Effects.CounterTargetingSourceUnlessPays(WardCost.Mana("{1}"))
        description = "Whenever you or another permanent you control becomes the target of a spell or " +
            "ability an opponent controls, counter that spell or ability unless its controller pays {1}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/back/3/6/3606519e-5677-4c21-a34e-be195b6669fa.jpg?1783928288"
        ruling("2021-02-05", "Valkmira's first ability prevents 1 damage from each source an opponent controls each time that source would deal damage to you. It prevents 1 of any kind of damage, not just combat damage.")
        ruling("2021-02-05", "Valkmira's last ability affects each spell (including Aura spells), activated ability, and triggered ability controlled by an opponent that targets you or another permanent you control.")
    }
}

val ReidaneGodOfTheWorthy: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = ReidaneGodOfTheWorthyFront,
    backFace = ValkmiraProtectorsShieldBack,
)
