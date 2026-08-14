package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Invasion of Innistrad // Deluge of the Dead — March of the Machine #115 (canonical printing).
 * {2}{B}{B} · Battle — Siege · defense 5 // Enchantment
 *
 * Flash
 * When this Siege enters, target creature an opponent controls gets -13/-13 until end of turn.
 * // When this enchantment enters, create two 2/2 black Zombie creature tokens.
 * // {2}{B}: Exile target card from a graveyard. If it was a creature card, create a 2/2 black
 * //   Zombie creature token.
 *
 * The reminder text a Siege prints ("As a Siege enters, choose an opponent to protect it. You and
 * others can attack it. When it's defeated, exile it, then cast it transformed.") is *not* card
 * text — it restates rules every battle has. So none of it is scripted here: the protector choice
 * is a state-based action (CR 704.5w/x), the attack legality is CR 310.8b, and the defeat trigger
 * is the intrinsic ability the engine grants every Siege
 * ([com.wingedsheep.sdk.scripting.Sieges.defeatAbility], CR 310.11b). The only things this card
 * declares are its `startingDefense` and its back face.
 *
 * -13/-13 is a kill clause, not a stat tweak — big enough to finish anything the format prints —
 * but it is still an ordinary until-end-of-turn P/T modification, so a creature with 14 toughness
 * survives and indestructible does not save one.
 */
private val InvasionOfInnistradFront = card("Invasion of Innistrad") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "Flash\n" +
        "When this Siege enters, target creature an opponent controls gets -13/-13 until end of turn."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.ModifyStats(-13, -13, victim)
        description = "When this Siege enters, target creature an opponent controls gets -13/-13 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Alexey Kruglov"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77720d2e-2b7b-492b-852c-eea5061eb31b.jpg?1783917010"
        ruling("2023-04-14", "Sieges each have an intrinsic triggered ability. That ability is \"When the last defense counter is removed from this permanent, exile it, then you may cast it transformed without paying its mana cost.\"")
        ruling("2023-04-14", "As a Siege enters the battlefield, its controller chooses an opponent to be its protector.")
        ruling("2023-04-14", "A battle can be attacked by all players other than its protector. Notably, this means a Siege's controller can attack it.")
        ruling("2023-04-14", "Damage dealt to a battle causes that many defense counters to be removed from it.")
        ruling("2023-04-14", "If a battle has no defense counters, and it isn't the source of a triggered ability that has triggered but not yet left the stack, that battle is put into its owner's graveyard. This is a state-based action. This doesn't cause a Siege's intrinsic triggered ability to trigger.")
        ruling("2023-04-14", "If a Siege never had defense counters on it (perhaps because a permanent became a copy of one), it can't have its last defense counter removed. It will be put into its owner's graveyard. You won't exile it or cast the other face.")
    }
}

/**
 * The back face. Cast transformed, for free, by the Siege's defeat trigger — so it has no mana
 * cost of its own and needs a colour indicator to be black off the battlefield.
 */
private val DelugeOfTheDead = card("Deluge of the Dead") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create two 2/2 black Zombie creature tokens.\n" +
        "{2}{B}: Exile target card from a graveyard. If it was a creature card, create a 2/2 " +
        "black Zombie creature token."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            count = 2,
        )
        description = "When this enchantment enters, create two 2/2 black Zombie creature tokens."
    }

    activatedAbility {
        cost = Costs.Mana(ManaCost.parse("{2}{B}"))
        val exiled = target("target card in a graveyard", Targets.CardInGraveyard)
        effect = Effects.Composite(
            Effects.Exile(exiled),
            // Reads the exiled card's printed type in exile, so it is true only when the card that
            // left the graveyard was a creature card — the Scavenging Ooze shape.
            ConditionalEffect(
                condition = Conditions.TargetIsCreatureCard(0),
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                ),
            ),
        )
        description = "{2}{B}: Exile target card from a graveyard. If it was a creature card, " +
            "create a 2/2 black Zombie creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Alexey Kruglov"
        flavorText = "The eerie magic animating Innistrad's zombies rendered them immune to " +
            "phyresis, making them crucial to the plane's defense."
        imageUri = "https://cards.scryfall.io/normal/back/7/7/77720d2e-2b7b-492b-852c-eea5061eb31b.jpg?1783917010"
    }
}

val InvasionOfInnistrad: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfInnistradFront,
    backFace = DelugeOfTheDead,
)
