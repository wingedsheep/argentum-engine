package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ojer Kaslem, Deepest Growth // Temple of Cultivation (The Lost Caverns of Ixalan)
 * {3}{G}{G}
 * Legendary Creature — God // Land
 *
 * Front — Ojer Kaslem, Deepest Growth (6/5, Trample)
 *   Whenever Ojer Kaslem deals combat damage to a player, reveal that many cards from the top
 *   of your library. You may put a creature card and/or a land card from among them onto the
 *   battlefield. Put the rest on the bottom of your library in a random order.
 *   When Ojer Kaslem dies, return it to the battlefield tapped and transformed under its
 *   owner's control.
 *
 * Back — Temple of Cultivation (Land)
 *   {T}: Add {G}.
 *   {2}{G}, {T}: Transform this land. Activate only if you control ten or more permanents and
 *   only as a sorcery.
 *
 * Implementation:
 *  - Combat-damage reveal reuses the Gishath, Sun's Avatar pipeline: [GatherCardsEffect] from the
 *    top of the library (N = [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT], revealed) → two
 *    [SelectionMode.ChooseUpTo]`(1)` selects, one filtered to [GameObjectFilter.Creature] and one
 *    to [GameObjectFilter.Land] chained on the first select's remainder (the "and/or" — up to one
 *    of each, and a creature-land picked as the creature can't be double-counted) → [MoveCollectionEffect]s
 *    put the picks onto the battlefield and bottom the rest in a random order.
 *  - Dies-return uses the shared [Effects.ReturnSelfFromGraveyardTransformed]`(tapped = true)`
 *    wired to [Triggers.Dies]: the God dies to the graveyard and the trigger returns it to the
 *    battlefield tapped, back face up (Temple of Cultivation).
 *  - Back land: `{T}: Add {G}` mana ability + a `{2}{G}, {T}` sorcery-speed [TransformEffect]
 *    gated on [Conditions.YouControlAtLeast]`(10, Permanent)`.
 */

private val OjerKaslemDeepestGrowthFront = card("Ojer Kaslem, Deepest Growth") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — God"
    power = 6
    toughness = 5
    oracleText = "Trample\n" +
        "Whenever Ojer Kaslem deals combat damage to a player, reveal that many cards from the " +
        "top of your library. You may put a creature card and/or a land card from among them " +
        "onto the battlefield. Put the rest on the bottom of your library in a random order.\n" +
        "When Ojer Kaslem dies, return it to the battlefield tapped and transformed under its " +
        "owner's control."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val damageDealt = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(count = damageDealt, player = Player.You),
                    storeAs = "kaslem_revealed",
                    revealed = true,
                ),
                SelectFromCollectionEffect(
                    from = "kaslem_revealed",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Creature,
                    showAllCards = true,
                    storeSelected = "kaslem_creature",
                    storeRemainder = "kaslem_afterCreature",
                    prompt = "You may put a creature card onto the battlefield",
                    selectedLabel = "Put onto the battlefield",
                ),
                SelectFromCollectionEffect(
                    from = "kaslem_afterCreature",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Land,
                    showAllCards = true,
                    storeSelected = "kaslem_land",
                    storeRemainder = "kaslem_toBottom",
                    prompt = "You may put a land card onto the battlefield",
                    selectedLabel = "Put onto the battlefield",
                    remainderLabel = "Put on the bottom of your library",
                ),
                MoveCollectionEffect(
                    from = "kaslem_creature",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                ),
                MoveCollectionEffect(
                    from = "kaslem_land",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                ),
                MoveCollectionEffect(
                    from = "kaslem_toBottom",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                    order = CardOrder.Random,
                ),
            )
        )
        description = "Whenever Ojer Kaslem deals combat damage to a player, reveal that many " +
            "cards from the top of your library. You may put a creature card and/or a land card " +
            "from among them onto the battlefield. Put the rest on the bottom of your library in " +
            "a random order."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ReturnSelfFromGraveyardTransformed(tapped = true)
        description = "When Ojer Kaslem dies, return it to the battlefield tapped and transformed " +
            "under its owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "204"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cbc43a3-8cba-4988-9de1-c89aedd79ada.jpg?1782694445"
    }
}

private val TempleOfCultivation = card("Temple of Cultivation") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "(Transforms from Ojer Kaslem, Deepest Growth.)\n" +
        "{T}: Add {G}.\n" +
        "{2}{G}, {T}: Transform this land. Activate only if you control ten or more permanents " +
        "and only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControlAtLeast(10, GameObjectFilter.Permanent)
            )
        )
        description = "Transform this land. Activate only if you control ten or more permanents " +
            "and only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "204"
        artist = "Ryan Pancoast"
        flavorText = "Chimil gave the Oltec sacred lands. Ojer Kaslem brought them to life."
        imageUri = "https://cards.scryfall.io/normal/back/0/c/0cbc43a3-8cba-4988-9de1-c89aedd79ada.jpg?1782694445"
    }
}

val OjerKaslemDeepestGrowth: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = OjerKaslemDeepestGrowthFront,
    backFace = TempleOfCultivation,
)
