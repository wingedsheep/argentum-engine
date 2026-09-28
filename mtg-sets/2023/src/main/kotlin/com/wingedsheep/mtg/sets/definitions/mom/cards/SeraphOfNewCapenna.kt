package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seraph of New Capenna // Seraph of New Phyrexia (March of the Machine #36)
 * {2}{W} Creature — Angel Soldier 2/2 // Creature — Phyrexian Angel 3/3 (white-black color indicator)
 *
 * Front — Flying. "{4}{B/P}: Transform this creature. Activate only as a sorcery."
 * Back  — Flying. "Whenever this creature attacks, you may sacrifice another creature or artifact.
 *         If you do, this creature gets +2/+1 until end of turn."
 *
 * The sacrifice is not targeted — it's chosen on resolution, so it's an optional cost via
 * [Effects.MayPay] over [Effects.SacrificeOwn] with `excludeSource` for "another".
 */
private val SeraphOfNewCapennaFront = card("Seraph of New Capenna") {
    manaCost = "{2}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Angel Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\n{4}{B/P}: Transform this creature. Activate only as a sorcery. " +
        "({B/P} can be paid with either {B} or 2 life.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{4}{B/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Aaron J. Riley"
        flavorText = "The angels had vowed to protect New Capenna. She was determined to uphold " +
            "that vow, no matter the cost."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbd9ed1d-1972-4db8-bc43-b05b94956aa0.jpg?1783917057"
    }
}

private val SeraphOfNewPhyrexia = card("Seraph of New Phyrexia") {
    manaCost = ""
    colorIndicator = "WB" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "WB"
    typeLine = "Creature — Phyrexian Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhenever this creature attacks, you may sacrifice another creature or " +
        "artifact. If you do, this creature gets +2/+1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(
                filter = GameObjectFilter.CreatureOrArtifact,
                count = 1,
                excludeSource = true,
            ),
            then = Effects.ModifyStats(2, 1, EffectTarget.Self),
        )
        description = "Whenever this creature attacks, you may sacrifice another creature or " +
            "artifact. If you do, this creature gets +2/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Aaron J. Riley"
        flavorText = "With her Halo reserves exhausted, she began to change, and she made a new " +
            "promise to bless New Capenna with glistening perfection."
        imageUri = "https://cards.scryfall.io/normal/back/f/b/fbd9ed1d-1972-4db8-bc43-b05b94956aa0.jpg?1783917057"
    }
}

val SeraphOfNewCapenna: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SeraphOfNewCapennaFront,
    backFace = SeraphOfNewPhyrexia,
)
