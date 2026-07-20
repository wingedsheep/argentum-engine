package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thousand Moons Smithy // Barracks of the Thousand (The Lost Caverns of Ixalan)
 * {2}{W}{W}
 * Legendary Artifact // Legendary Artifact Land
 *
 * Front — Thousand Moons Smithy ({2}{W}{W} Legendary Artifact)
 *   When Thousand Moons Smithy enters, create a white Gnome Soldier artifact creature token with
 *   "This token's power and toughness are each equal to the number of artifacts and/or creatures
 *   you control."
 *   At the beginning of your first main phase, you may tap five untapped artifacts and/or creatures
 *   you control. If you do, transform Thousand Moons Smithy.
 *
 * Back — Barracks of the Thousand (Legendary Artifact Land)
 *   {T}: Add {W}.
 *   Whenever you cast an artifact or creature spell using mana produced by Barracks of the Thousand,
 *   create a white Gnome Soldier artifact creature token (same characteristic-defining P/T).
 *
 * Implementation:
 *  - The token's self-referential P/T is a [SetBasePowerToughnessDynamicStatic] CDA (Layer 7b) on
 *    the token itself ([GroupFilter.source]), counting artifacts and/or creatures you control — so it
 *    updates continuously, not a snapshot at creation (BonnyPall Clearcutter's idiom). The same token
 *    factory feeds both the front ETB and the back cast trigger.
 *  - The first-main "you may tap five … If you do, transform" is an [OptionalCostEffect] whose payable
 *    cost is the Gather → Select-exactly-5 → Tap pipeline (Caparocti Sunborn's idiom), transforming on
 *    payment.
 *  - Barracks' cast trigger uses [SpellCastPredicate.PaidWithManaFromSource] — the mana-source
 *    provenance the engine records for the mana Barracks produced.
 */

/** Count of artifacts and/or creatures you control (an artifact creature counts once). */
private val artifactsAndCreaturesYouControl: DynamicAmount =
    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact or GameObjectFilter.Creature).count()

/** Create the white Gnome Soldier artifact creature token with the characteristic-defining P/T. */
private fun gnomeSoldierToken() = Effects.CreateToken(
    power = 0,
    toughness = 0,
    colors = setOf(Color.WHITE),
    creatureTypes = setOf("Gnome", "Soldier"),
    artifactToken = true,
    imageUri = "https://cards.scryfall.io/normal/front/4/a/4a6bec46-1acd-4726-b8d9-3045ac6a2ea2.jpg?1782694580",
    staticAbilities = listOf(
        SetBasePowerToughnessDynamicStatic(
            power = artifactsAndCreaturesYouControl,
            toughness = artifactsAndCreaturesYouControl,
            filter = GroupFilter.source(),
        )
    ),
)

private val ThousandMoonsSmithyFront = card("Thousand Moons Smithy") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact"
    oracleText = "When Thousand Moons Smithy enters, create a white Gnome Soldier artifact creature " +
        "token with \"This token's power and toughness are each equal to the number of artifacts " +
        "and/or creatures you control.\"\n" +
        "At the beginning of your first main phase, you may tap five untapped artifacts and/or " +
        "creatures you control. If you do, transform Thousand Moons Smithy."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = gnomeSoldierToken()
        description = "When Thousand Moons Smithy enters, create a white Gnome Soldier artifact " +
            "creature token with \"This token's power and toughness are each equal to the number " +
            "of artifacts and/or creatures you control.\""
    }

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        val tapCost = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = (GameObjectFilter.Artifact or GameObjectFilter.Creature).untapped(),
                    ),
                    storeAs = "smithyTapPool",
                ),
                SelectFromCollectionEffect(
                    from = "smithyTapPool",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(5)),
                    storeSelected = "smithyToTap",
                    prompt = "Tap five untapped artifacts and/or creatures you control",
                    useTargetingUI = true,
                ),
                TapUntapCollectionEffect("smithyToTap", tap = true),
            ),
        )
        effect = OptionalCostEffect(
            cost = tapCost,
            ifPaid = TransformEffect(EffectTarget.Self),
            descriptionOverride = "You may tap five untapped artifacts and/or creatures you control. " +
                "If you do, transform Thousand Moons Smithy.",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a6bec46-1acd-4726-b8d9-3045ac6a2ea2.jpg?1782694580"
    }
}

private val BarracksOfTheThousand = card("Barracks of the Thousand") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Legendary Artifact Land"
    oracleText = "{T}: Add {W}.\n" +
        "Whenever you cast an artifact or creature spell using mana produced by Barracks of the " +
        "Thousand, create a white Gnome Soldier artifact creature token with \"This token's power " +
        "and toughness are each equal to the number of artifacts and/or creatures you control.\""

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
            requires = setOf(SpellCastPredicate.PaidWithManaFromSource),
        )
        effect = gnomeSoldierToken()
        description = "Whenever you cast an artifact or creature spell using mana produced by " +
            "Barracks of the Thousand, create a white Gnome Soldier artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/back/4/a/4a6bec46-1acd-4726-b8d9-3045ac6a2ea2.jpg?1782694580"
    }
}

val ThousandMoonsSmithy: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ThousandMoonsSmithyFront,
    backFace = BarracksOfTheThousand,
)
