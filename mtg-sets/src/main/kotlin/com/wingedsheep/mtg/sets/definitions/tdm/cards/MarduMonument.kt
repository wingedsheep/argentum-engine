package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mardu Monument — Tarkir: Dragonstorm #245
 * {2} · Artifact
 *
 * When this artifact enters, search your library for a basic Mountain, Plains, or Swamp card,
 * reveal it, put it into your hand, then shuffle.
 * {2}{R}{W}{B}, {T}, Sacrifice this artifact: Create three 1/1 red Warrior creature tokens. They
 * gain menace and haste until end of turn. Activate only as a sorcery.
 *
 * The ETB is a mandatory single-card library search restricted to basic lands carrying one of the
 * three Mardu basic subtypes, revealed and placed into hand then shuffled (atomic
 * [EffectPatterns.searchLibrary]). The sacrifice ability creates three Warrior tokens, then grants
 * each menace and haste until end of turn via the [CREATED_TOKENS] pipeline — the keywords are a
 * temporary grant (not intrinsic) so menace does not wrongly persist if a token survives the turn.
 */
val MarduMonument = card("Mardu Monument") {
    manaCost = "{2}"
    colorIdentity = "RWB"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, search your library for a basic Mountain, Plains, or Swamp card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "{2}{R}{W}{B}, {T}, Sacrifice this artifact: Create three 1/1 red Warrior creature tokens. They " +
        "gain menace and haste until end of turn. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                listOf(Subtype.MOUNTAIN, Subtype.PLAINS, Subtype.SWAMP)
            ),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
        description = "When this artifact enters, search your library for a basic Mountain, Plains, or Swamp card, " +
            "reveal it, put it into your hand, then shuffle."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}{W}{B}"), Costs.Tap, Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        effect = CompositeEffect(
            buildList {
                add(
                    Effects.CreateToken(
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.RED),
                        creatureTypes = setOf("Warrior"),
                        count = 3,
                        imageUri = "https://cards.scryfall.io/normal/front/7/e/7edc0515-a130-45a7-aa09-0e23bba41587.jpg?1742506712"
                    )
                )
                // Grant menace and haste until end of turn to each freshly-created token.
                for (index in 0 until 3) {
                    add(
                        Effects.GrantKeyword(
                            Keyword.MENACE,
                            EffectTarget.PipelineTarget(CREATED_TOKENS, index),
                            Duration.EndOfTurn
                        )
                    )
                    add(
                        Effects.GrantKeyword(
                            Keyword.HASTE,
                            EffectTarget.PipelineTarget(CREATED_TOKENS, index),
                            Duration.EndOfTurn
                        )
                    )
                }
            }
        )
        description = "{2}{R}{W}{B}, {T}, Sacrifice this artifact: Create three 1/1 red Warrior creature tokens. " +
            "They gain menace and haste until end of turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Salvatorre Zee Yazzie"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bd0c794-77bc-4d4a-a769-3829e2ce4bdf.jpg?1743204968"
    }
}
