package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Master of Winds — Zendikar Rising #68
 * {2}{U}{U} · Creature — Sphinx Wizard · 1/4
 *
 * Flying
 * When this creature enters, draw two cards, then discard a card.
 * Whenever you cast an instant, sorcery, or Wizard spell, you may have this creature's base power
 * and toughness become 4/1 or 1/4 until end of turn.
 *
 * Per the ruling, nothing is chosen as the cast trigger fires: on resolution the controller picks
 * 4/1, 1/4, or no change — one [Effects.ChooseAction] with an explicit "no change" option
 * ([Effects.Nothing]) rather than a May prompt followed by a second choice. Either stat choice is
 * [Effects.SetBasePowerAndToughness] (Layer 7b), so counters and pumps still apply on top and a
 * later set-value effect overwrites it by timestamp.
 */
val MasterOfWinds = card("Master of Winds") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx Wizard"
    oracleText = "Flying\n" +
        "When this creature enters, draw two cards, then discard a card.\n" +
        "Whenever you cast an instant, sorcery, or Wizard spell, you may have this creature's base " +
        "power and toughness become 4/1 or 1/4 until end of turn."
    power = 1
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(2) then Effects.Discard(1)
        description = "When this creature enters, draw two cards, then discard a card."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery or GameObjectFilter.Any.withSubtype("Wizard")
        )
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    label = "Become 4/1 until end of turn",
                    effect = Effects.SetBasePowerAndToughness(4, 1, EffectTarget.Self),
                ),
                EffectChoice(
                    label = "Become 1/4 until end of turn",
                    effect = Effects.SetBasePowerAndToughness(1, 4, EffectTarget.Self),
                ),
                EffectChoice(label = "Don't change", effect = Effects.Nothing),
            )
        )
        description = "Whenever you cast an instant, sorcery, or Wizard spell, you may have this " +
            "creature's base power and toughness become 4/1 or 1/4 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Yigit Koroglu"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0caaf92a-3d9d-4c52-acff-cf960ec55e47.jpg?1783929392"
        ruling("2020-09-25", "You don't choose a power and toughness for Master of Winds as its ability triggers. Rather, as it resolves, you choose whether to have Master of Winds become 4/1, become 1/4, or not change its power and toughness.")
        ruling("2020-09-25", "The ability of Master of Winds overwrites any previous effects that set its power and/or toughness to specific values. Other effects that set these characteristics to specific values that start to apply after the ability resolves will overwrite that part of the effect.")
        ruling("2020-09-25", "Effects that modify the power or toughness of Master of Winds without setting it will apply to its new base power and toughness no matter when they started to take effect. The same is true for counters that change its power and toughness.")
        ruling("2020-09-25", "A Wizard spell is one with the creature type Wizard. Spells that are Wizard-themed (such as Relic Amulet) aren't Wizard spells.")
    }
}
