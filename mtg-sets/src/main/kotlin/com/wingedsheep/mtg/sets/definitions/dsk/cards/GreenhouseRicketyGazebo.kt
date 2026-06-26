package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Greenhouse // Rickety Gazebo (DSK 181) — split-layout Room (CR 709.5).
 *
 * Greenhouse {2}{G} — Enchantment — Room
 *   Lands you control have "{T}: Add one mana of any color."
 *
 * Rickety Gazebo {3}{G} — Enchantment — Room
 *   When you unlock this door, mill four cards, then return up to two permanent cards from among
 *   them to your hand.
 *
 * Cast either half; the cast face enters unlocked, the other locked. Pay the locked face's printed
 * mana cost as a sorcery-speed special action to unlock it (CR 709.5e).
 *
 * Greenhouse is a *static* ability printed on a Room face, so it functions only while the Greenhouse
 * door is unlocked (CR 709.5). It grants every land its controller controls a granted mana ability
 * via [GrantActivatedAbility]; the engine surfaces and pays with that grant through the Room-face-
 * aware static-ability projection (see RoomFaceStatics) — both as a clickable mana ability and to
 * the auto-payer. Rickety Gazebo's [Triggers.OnDoorUnlocked] trigger mills four and returns up to
 * two permanent cards from among them (the Cache Grab gather→mill→select→return pipeline, here with
 * up to two picks).
 */
val GreenhouseRicketyGazebo = card("Greenhouse // Rickety Gazebo") {
    layout = CardLayout.SPLIT
    colorIdentity = "G"

    face("Greenhouse") {
        manaCost = "{2}{G}"
        typeLine = "Enchantment — Room"
        oracleText = "Lands you control have \"{T}: Add one mana of any color.\""

        staticAbility {
            ability = GrantActivatedAbility(
                ability = ActivatedAbility(
                    id = AbilityId.generate(),
                    cost = Costs.Tap,
                    effect = Effects.AddManaOfChoice(),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility
                ),
                filter = GroupFilter(GameObjectFilter.Land.youControl())
            )
        }
    }

    face("Rickety Gazebo") {
        manaCost = "{3}{G}"
        typeLine = "Enchantment — Room"
        oracleText = "When you unlock this door, mill four cards, then return up to two permanent " +
            "cards from among them to your hand."

        triggeredAbility {
            trigger = Triggers.OnDoorUnlocked
            effect = Effects.Composite(
                listOf(
                    // Mill four: gather the top four, move them to the graveyard.
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                        storeAs = "milled"
                    ),
                    MoveCollectionEffect(
                        from = "milled",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD)
                    ),
                    // Return up to two permanent cards from among the milled cards to your hand.
                    SelectFromCollectionEffect(
                        from = "milled",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                        filter = GameObjectFilter.Permanent,
                        storeSelected = "selected",
                        showAllCards = true,
                        prompt = "Return up to two permanent cards to your hand",
                        selectedLabel = "Return to hand",
                        remainderLabel = "Leave in graveyard"
                    ),
                    MoveCollectionEffect(
                        from = "selected",
                        destination = CardDestination.ToZone(Zone.HAND)
                    )
                )
            )
            description = "When you unlock this door, mill four cards, then return up to two " +
                "permanent cards from among them to your hand."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "John Di Giovanni"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a22e8038-0706-47ec-bfda-f421a4912774.jpg?1726780671"
    }
}
