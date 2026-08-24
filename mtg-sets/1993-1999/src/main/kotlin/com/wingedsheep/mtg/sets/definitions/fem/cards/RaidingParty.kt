package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerCollectingEffect
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Raiding Party
 * {2}{R}
 * Enchantment
 * This enchantment can't be the target of white spells or abilities from white sources.
 * Sacrifice an Orc: Each player may tap any number of untapped white creatures they control. For
 * each creature tapped this way, that player chooses up to two Plains. Then destroy all Plains that
 * weren't chosen this way by any player.
 *
 * The set's most elaborate card, and it is entirely a bookkeeping problem: every player gets their
 * own tap-and-spare decision, the spared Plains have to *accumulate* across players, and what is
 * destroyed is the complement of that accumulated set — including Plains belonging to players who
 * spared none.
 *
 * Three pipeline pieces carry it: [ForEachPlayerCollectingEffect] runs a fresh sub-pipeline per
 * player in APNAP order and appends each player's picks into one shared collection; the
 * spare-count is the selection's own `_count` doubled; and a `FilterCollectionEffect` over
 * `CollectionFilter.ExcludeOtherCollection` takes the battlefield's Plains *minus* everything spared.
 *
 * Note "up to two Plains" is not "up to two of your Plains" — a player may spare an opponent's, and
 * a player who taps nothing simply spares nothing.
 */
val RaidingParty = card("Raiding Party") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "This enchantment can't be the target of white spells or abilities from white sources.\n" +
        "Sacrifice an Orc: Each player may tap any number of untapped white creatures they " +
        "control. For each creature tapped this way, that player chooses up to two Plains. Then " +
        "destroy all Plains that weren't chosen this way by any player."

    keywordAbility(KeywordAbility.hexproofFrom(Color.WHITE))

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.ORC))
        effect = Effects.Composite(
            ForEachPlayerCollectingEffect(
                players = Player.ActivePlayerFirst,
                effects = listOf(
                    GatherCardsEffect(
                        source = CardSource.ControlledPermanents(
                            player = Player.You,
                            filter = GameObjectFilter.Creature.withColor(Color.WHITE).untapped()
                        ),
                        storeAs = "tappable"
                    ),
                    SelectFromCollectionEffect(
                        from = "tappable",
                        selection = SelectionMode.ChooseAnyNumber,
                        chooser = Chooser.Controller,
                        storeSelected = "tapped",
                        useTargetingUI = true,
                        prompt = "Tap any number of untapped white creatures you control (each spares two Plains)"
                    ),
                    ForEachInCollectionEffect(
                        collection = "tapped",
                        effect = Effects.Tap(EffectTarget.Self)
                    ),
                    GatherCardsEffect(
                        source = CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.Land.withSubtype(Subtype.PLAINS)
                        ),
                        storeAs = "plains"
                    ),
                    SelectFromCollectionEffect(
                        from = "plains",
                        selection = SelectionMode.ChooseUpTo(
                            DynamicAmount.Multiply(DynamicAmount.VariableReference("tapped_count"), 2)
                        ),
                        chooser = Chooser.Controller,
                        storeSelected = "spared",
                        useTargetingUI = true,
                        prompt = "Choose up to two Plains for each creature you tapped"
                    ),
                ),
                collectCollections = mapOf("spared" to "allSpared"),
            ),
            // Everything left over — the complement of what every player spared between them.
            // Gather every Plains, then subtract the accumulated picks: the set difference is
            // `CollectionFilter.ExcludeOtherCollection`, which is what that filter exists for.
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Land.withSubtype(Subtype.PLAINS)
                ),
                storeAs = "allPlains",
            ),
            FilterCollectionEffect(
                from = "allPlains",
                filter = CollectionFilter.ExcludeOtherCollection("allSpared"),
                storeMatching = "doomed",
            ),
            MoveCollectionEffect(
                from = "doomed",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy,
            ),
        )
        description = "Sacrifice an Orc: Each player may tap any number of untapped white creatures they control. For each creature tapped this way, that player chooses up to two Plains. Then destroy all Plains that weren't chosen this way by any player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/907a3396-706b-4ca2-9973-bca758986032.jpg?1783947890"
    }
}
