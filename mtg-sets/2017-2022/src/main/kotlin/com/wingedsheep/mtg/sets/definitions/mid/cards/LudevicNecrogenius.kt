package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.BecomesCopyAsTransforms
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ludevic, Necrogenius // Olag, Ludevic's Hubris (Innistrad: Midnight Hunt)
 * {U}{B}
 * Legendary Creature — Human Wizard 2/3 // Legendary Creature — Zombie 4/4
 *
 * Front — Whenever Ludevic enters or attacks, mill a card. "{X}{U}{U}{B}{B}, Exile X creature cards
 *         from your graveyard: Transform Ludevic. X can't be 0. Activate only as a sorcery."
 * Back  — As this creature transforms into Olag, it becomes a copy of a creature card exiled with
 *         it, except its name is Olag, Ludevic's Hubris, it's 4/4, and it's a legendary blue and
 *         black Zombie in addition to its other colors and types. Put a number of +1/+1 counters on
 *         Olag equal to the number of creature cards exiled with it.
 *
 * The exile cost links its cards to Ludevic (`linkToSource`, CR 607.2a), so Olag's
 * [BecomesCopyAsTransforms] reads them back with `exiledWithSource()`. "X can't be 0" is
 * `minimumXValue = 1`; the {X} mana fixes how many cards the cost exiles.
 */

private const val MILL_TEXT = "Whenever Ludevic enters or attacks, mill a card."

private val creatureCardsExiledWithIt = GameObjectFilter.Creature.exiledWithSource()

private val LudevicFront = card("Ludevic, Necrogenius") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "$MILL_TEXT\n" +
        "{X}{U}{U}{B}{B}, Exile X creature cards from your graveyard: Transform Ludevic. X can't be 0. " +
        "Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(1)
        description = MILL_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Library.mill(1)
        description = MILL_TEXT
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{X}{U}{U}{B}{B}"),
            Costs.ExileXFromGraveyard(GameObjectFilter.Creature, linkToSource = true),
        )
        effect = Effects.Transform(EffectTarget.Self)
        minimumXValue = 1
        timing = TimingRule.SorcerySpeed
        description = "{X}{U}{U}{B}{B}, Exile X creature cards from your graveyard: Transform Ludevic. " +
            "X can't be 0. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/788288f6-7944-48f4-91b0-f452e209c9ce.jpg?1783925561"
        ruling("2021-09-24", "You choose which exiled creature card Olag is copying as it transforms. It doesn't transform and then become a copy.")
        ruling("2021-09-24", "Olag remains a copy of the creature card you choose even if that card later leaves exile.")
        ruling("2021-09-24", "If there are no creature cards exiled with Ludevic as it transforms, perhaps because those cards were removed from exile in response to its activated ability or it transformed another way, Olag won't be a copy of anything. Olag will simply be its 4/4 self with no +1/+1 counters.")
        ruling("2021-09-24", "It's possible for Olag to become a copy of a card with the daybound keyword ability as it transforms. If this happens, it doesn't transform as it becomes night.")
        ruling("2021-09-24", "If Olag becomes a copy of another transforming double-faced card that doesn't have nightbound, such as Delver of Secrets, abilities of it gains or other effects may cause it to transform. If it transforms this way, it does not become Ludevic again. The copy effect continues to apply and its characteristics don't change. If the creature it is a copy of has a triggered ability that occurs when it transforms, that ability will trigger.")
    }
}

private val OlagLudevicsHubris = card("Olag, Ludevic's Hubris") {
    manaCost = ""
    colorIdentity = "UB"
    colorIndicator = "UB" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Zombie"
    power = 4
    toughness = 4
    oracleText = "As this creature transforms into Olag, Ludevic's Hubris, it becomes a copy of a creature " +
        "card exiled with it, except its name is Olag, Ludevic's Hubris, it's 4/4, and it's a legendary " +
        "blue and black Zombie in addition to its other colors and types. Put a number of +1/+1 counters " +
        "on Olag equal to the number of creature cards exiled with it."

    replacementEffect(
        BecomesCopyAsTransforms(
            copyFilter = creatureCardsExiledWithIt,
            copyFromZone = Zone.EXILE,
            exceptions = CopyExceptions(
                nameOverride = "Olag, Ludevic's Hubris",
                powerOverride = 4,
                toughnessOverride = 4,
                addedSupertypes = setOf(Supertype.LEGENDARY),
                addedColors = setOf(Color.BLUE, Color.BLACK),
                addedSubtypes = setOf(Subtype.ZOMBIE),
            ),
            counterCount = DynamicAmounts.count(Player.Each, Zone.EXILE, creatureCardsExiledWithIt),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/back/7/8/788288f6-7944-48f4-91b0-f452e209c9ce.jpg?1783925561"
    }
}

val LudevicNecrogenius: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = LudevicFront,
    backFace = OlagLudevicsHubris,
)
