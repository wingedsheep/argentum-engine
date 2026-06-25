package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Orphans of the Wheat — Duskmourn: House of Horror #22
 * {1}{W} · Creature — Human · 2/1
 *
 * Whenever this creature attacks, tap any number of untapped creatures you control. This creature
 * gets +1/+1 until end of turn for each creature tapped this way.
 *
 * "Tap any number" (zero allowed, so no "may" is needed) is the Gather → Select → Tap pipeline:
 * gather your untapped creatures, choose any number of them on the battlefield, tap that
 * selection, then pump this creature. The buff reads the auto-derived selection count
 * (`DynamicAmount.VariableReference("tapped_count")`), which `ModifyStatsExecutor` snapshots into a
 * fixed +N/+N floating effect lasting until end of turn — so later untapping doesn't change it.
 * The attacker itself was tapped by attacking (no vigilance), so it isn't in the untapped pool.
 */
val OrphansOfTheWheat = card("Orphans of the Wheat") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks, tap any number of untapped creatures you " +
        "control. This creature gets +1/+1 until end of turn for each creature tapped this way."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Creature.youControl().untapped()
                    ),
                    storeAs = "candidates"
                ),
                SelectFromCollectionEffect(
                    from = "candidates",
                    selection = SelectionMode.ChooseAnyNumber,
                    storeSelected = "tapped",
                    prompt = "Tap any number of untapped creatures you control",
                    useTargetingUI = true
                ),
                TapUntapCollectionEffect("tapped", tap = true),
                Effects.ModifyStats(
                    power = DynamicAmount.VariableReference("tapped_count"),
                    toughness = DynamicAmount.VariableReference("tapped_count"),
                    target = EffectTarget.Self
                )
            )
        )
        description = "Whenever this creature attacks, tap any number of untapped creatures you " +
            "control. This creature gets +1/+1 until end of turn for each creature tapped this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Julie Dillon"
        flavorText = "Their only family is each other, and their only toys are those who wander too far into the fields."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ef4aab1-8bc0-4652-91d7-3e8b20b411ad.jpg?1726285941"
    }
}
