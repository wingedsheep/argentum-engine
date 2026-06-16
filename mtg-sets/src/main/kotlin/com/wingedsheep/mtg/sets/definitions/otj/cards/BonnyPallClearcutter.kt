package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bonny Pall, Clearcutter {3}{G}{U}{U}
 * Legendary Creature — Giant Scout
 * 6/5
 *
 * Reach
 * When Bonny Pall enters, create Beau, a legendary blue Ox creature token with "Beau's power
 * and toughness are each equal to the number of lands you control."
 * Whenever you attack, draw a card, then you may put a land card from your hand or graveyard
 * onto the battlefield.
 *
 * Beau's star/star is a characteristic-defining ability (CR 604.3): it recomputes continuously, so
 * it is modeled with a [SetBasePowerToughnessDynamicStatic] on the token (Layer 7b SET_VALUES)
 * rather than snapshotting the land count via `CreateTokenEffect.dynamicPower` at creation time.
 *
 * The attack trigger's "you may put a land card from your hand or graveyard" is an optional
 * "up to one" selection ([SelectionMode.ChooseUpTo]) gathered from both zones at once
 * ([CardSource.FromMultipleZones]); the client groups the choices by zone (MultiZoneSelectionUI).
 */
val BonnyPallClearcutter = card("Bonny Pall, Clearcutter") {
    manaCost = "{3}{G}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Giant Scout"
    oracleText = "Reach\n" +
        "When Bonny Pall enters, create Beau, a legendary blue Ox creature token with " +
        "\"Beau's power and toughness are each equal to the number of lands you control.\"\n" +
        "Whenever you attack, draw a card, then you may put a land card from your hand or " +
        "graveyard onto the battlefield."
    power = 6
    toughness = 5
    keywords(Keyword.REACH)

    // When Bonny Pall enters, create Beau (a legendary blue Ox with a CDA P/T).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(1),
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Ox"),
            name = "Beau",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/1/f1751016-9461-4250-ac07-afe4f5b41095.jpg?1712316157",
            staticAbilities = listOf(
                SetBasePowerToughnessDynamicStatic(
                    power = DynamicAmounts.landsYouControl(),
                    toughness = DynamicAmounts.landsYouControl(),
                    filter = GroupFilter.source()
                )
            )
        )
    }

    // Whenever you attack, draw a card, then you may put a land card from your hand or
    // graveyard onto the battlefield.
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Effects.Composite(
            Effects.DrawCards(1),
            GatherCardsEffect(
                source = CardSource.FromMultipleZones(
                    zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                    player = Player.You,
                    filter = GameObjectFilter.Land
                ),
                storeAs = "bonnyPallLands"
            ),
            SelectFromCollectionEffect(
                from = "bonnyPallLands",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "bonnyPallLandToPlay"
            ),
            MoveCollectionEffect(
                from = "bonnyPallLandToPlay",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4383ae7c-58ea-4354-93e4-677ad185c3bb.jpg?1712356061"
    }
}
