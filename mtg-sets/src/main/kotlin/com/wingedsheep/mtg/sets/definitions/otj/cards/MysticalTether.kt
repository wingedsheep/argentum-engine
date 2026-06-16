package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Mystical Tether
 * {2}{W}
 * Enchantment
 * You may cast this spell as though it had flash if you pay {2} more to cast it.
 * When this enchantment enters, exile target artifact or creature an opponent controls
 * until this enchantment leaves the battlefield.
 *
 * Banishing Light shape: an ETB exile-until-leaves linked to the source's departure
 * ([Effects.ExileUntilLeaves] / [Effects.ReturnLinkedExileUnderOwnersControl]), here
 * scoped to "artifact or creature an opponent controls" ([GameObjectFilter.CreatureOrArtifact]).
 * The "pay {2} more to cast as though it had flash" rider is the Rout / Ghitu Fire pattern:
 * [KeywordAbility.flashKicker] — paying the extra cost unlocks instant-speed casting without
 * otherwise changing the spell.
 */
val MysticalTether = card("Mystical Tether") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "You may cast this spell as though it had flash if you pay {2} more to cast it.\n" +
        "When this enchantment enters, exile target artifact or creature an opponent controls " +
        "until this enchantment leaves the battlefield."

    keywordAbility(KeywordAbility.flashKicker("{2}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "artifact or creature an opponent controls",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls()))
        )
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Adam Volker"
        flavorText = "Kellan had no plan. He simply poured his focus into the train and willed it to stop."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18344498-952e-489c-8b03-bd1bef4c26ca.jpg?1712355302"
    }
}
