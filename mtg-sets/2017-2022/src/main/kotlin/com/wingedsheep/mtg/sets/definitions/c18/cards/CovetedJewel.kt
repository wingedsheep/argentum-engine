package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Coveted Jewel — Commander 2018 #54
 * {6} · Artifact
 *
 * When this artifact enters, draw three cards.
 * {T}: Add three mana of any one color.
 * Whenever one or more creatures an opponent controls attack you and aren't blocked, that player
 * draws three cards and gains control of this artifact. Untap it.
 *
 * "Attack you and aren't blocked" is `Triggers.you.isAttacked(unblocked = true)`: it fires once,
 * after your block declaration, and binds the attackers' controller as [Player.TriggeringPlayer].
 * Creatures attacking you are always an opponent's, so the trigger needs no controller filter.
 */
val CovetedJewel = card("Coveted Jewel") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw three cards.\n" +
        "{T}: Add three mana of any one color.\n" +
        "Whenever one or more creatures an opponent controls attack you and aren't blocked, that player " +
        "draws three cards and gains control of this artifact. Untap it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3)
        description = "When this artifact enters, draw three cards."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add three mana of any one color."
    }

    triggeredAbility {
        trigger = Triggers.you.isAttacked(unblocked = true)
        effect = Effects.DrawCards(3, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.GiveControl(
                permanent = EffectTarget.Self,
                newController = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            ) then
            Effects.Untap(EffectTarget.Self)
        description = "Whenever one or more creatures an opponent controls attack you and aren't blocked, " +
            "that player draws three cards and gains control of this artifact. Untap it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f83ed433-fae3-4fa5-acad-bb8a5b535ce3.jpg?1783934324"
    }
}
