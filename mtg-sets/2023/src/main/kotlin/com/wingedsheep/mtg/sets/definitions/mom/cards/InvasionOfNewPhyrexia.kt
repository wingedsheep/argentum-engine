package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasion of New Phyrexia // Teferi Akosa of Zhalfir — March of the Machine #239 (canonical).
 * {X}{W}{U} · Battle — Siege · defense 6 // Legendary Planeswalker — Teferi · loyalty 4
 *
 * Front: the enters trigger has no X of its own, so the token count reads the X the battle was
 * cast with off the permanent ([DynamicAmounts.castX]), as Invasion of Ikoria does.
 *
 * Back:
 *  - **+1** is Thirst for Knowledge's "discard two unless you discard a [type] card" — one
 *    selection, [Effects.DiscardUnlessMatching].
 *  - **−2** is a permanent emblem whose group gets +1/+0 and owns a [GrantWard] static. The engine
 *    reads an emblem-owned `GrantWard` for both the WARD keyword it projects onto the group and the
 *    ward trigger itself, so `grantedKeywords` stays empty.
 *  - **−3** taps any number of untapped creatures as the ability resolves (zero is allowed — per
 *    the ruling, X is then 0), then a reflexive "when you do" trigger (CR 603.12) goes on the
 *    stack with its target chosen then. X is the size of the tapped collection, carried from the
 *    action's pipeline to the reflexive trigger's target filter and its resolution re-check.
 */
private val tappedCreatures = CollectionSlot("tappedCreatures")

private val InvasionOfNewPhyrexiaFront = card("Invasion of New Phyrexia") {
    manaCost = "{X}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Battle — Siege"
    startingDefense = 6
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, create X 2/2 white and blue Knight creature tokens with vigilance."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.castX(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE, Color.BLUE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/8/8/88439bfc-8942-473b-9e4f-863017788476.jpg?1783916669"
        )
        description = "When this Siege enters, create X 2/2 white and blue Knight creature tokens " +
            "with vigilance."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "239"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b83af86-e1a9-4be1-83dc-0c1ab81b129e.jpg?1783916950"
    }
}

private val knightsYouControl = GroupFilter(
    GameObjectFilter.Creature.withSubtype(Subtype.KNIGHT).youControl()
)

private val TeferiAkosaOfZhalfir = card("Teferi Akosa of Zhalfir") {
    manaCost = ""
    colorIdentity = "WU"
    colorIndicator = "WU"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 4
    oracleText = "+1: Draw two cards. Then discard two cards unless you discard a creature card.\n" +
        "−2: You get an emblem with \"Knights you control get +1/+0 and have ward {1}.\"\n" +
        "−3: Tap any number of untapped creatures you control. When you do, shuffle target " +
        "nonland permanent an opponent controls with mana value X or less into its owner's " +
        "library, where X is the number of creatures tapped this way."

    loyaltyAbility(+1) {
        effect = Effects.DrawCards(2) then Effects.DiscardUnlessMatching(2, GameObjectFilter.Creature)
    }

    loyaltyAbility(-2) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = knightsYouControl,
            powerBonus = 1,
            ownedStaticAbilities = listOf(GrantWard(WardCost.Mana("{1}"), knightsYouControl)),
            emblemDescription = "Knights you control get +1/+0 and have ward {1}."
        )
    }

    loyaltyAbility(-3) {
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val untapped = gather(
                    CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature.untapped())
                )
                val tapped = chooseAnyNumber(
                    from = untapped,
                    chooser = Chooser.Controller,
                    useTargetingUI = true,
                    prompt = "Tap any number of untapped creatures you control",
                    name = "tappedCreatures",
                )
                run(Effects.TapCollection(tapped, tap = true))
            },
            optional = false,
            descriptionOverride = "Tap any number of untapped creatures you control. When you do, " +
                "shuffle target nonland permanent an opponent controls with mana value X or less " +
                "into its owner's library, where X is the number of creatures tapped this way."
        ) {
            val permanent = target(
                TargetFilter.NonlandPermanentOpponentControls
                    .manaValueAtMostDynamic(tappedCreatures.count)
            )
            effect = Effects.ShuffleIntoLibrary(permanent)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "239"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/back/1/b/1b83af86-e1a9-4be1-83dc-0c1ab81b129e.jpg?1783916950"
        ruling(
            "2023-04-14",
            "You activate Teferi Akosa's last ability without choosing a target or tapping any " +
                "creatures. As it resolves, you choose which untapped creatures you control to tap, " +
                "if any. If you don't tap any creatures (including if you don't control any), X is 0. " +
                "After you tap creatures or not, the reflexive triggered ability triggers. You choose " +
                "the target for that ability as it's put on the stack."
        )
    }
}

val InvasionOfNewPhyrexia: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfNewPhyrexiaFront,
    backFace = TeferiAkosaOfZhalfir,
)
